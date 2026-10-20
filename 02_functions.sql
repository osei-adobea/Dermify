-- Dermify curation pipeline — ingest functions
--
-- The app never inserts into `contributions` directly and never reads from it.
-- Both operations go through these SECURITY DEFINER functions, which is what
-- makes acceptance criterion 4 achievable: a contributor's client can trigger a
-- duplicate check without being granted SELECT on other people's rows.

-- ---------------------------------------------------------------------------
-- Hamming distance between two 16-hex-char perceptual hashes.
--
-- ('x' || h)::bit(64) reinterprets the hex text as 64 bits; XOR then counting
-- the set bits gives the distance. Threshold 5 comes from the training
-- pipeline's measurements: true duplicates hash to distance 0 even after
-- resize and JPEG recompression, distinct images sit at 14 or more.
-- ---------------------------------------------------------------------------
create or replace function phash_distance(a text, b text)
returns int
language sql
immutable
strict
parallel safe
as $$
  select length(replace((('x' || a)::bit(64) # ('x' || b)::bit(64))::text, '0', ''));
$$;

-- ---------------------------------------------------------------------------
-- Turns a label into a safe, predictable storage folder name: lowercase,
-- non-alphanumeric runs collapsed to a single underscore, leading/trailing
-- underscores trimmed. A label that sanitizes to nothing (empty string, or
-- entirely punctuation) falls back to 'unlabeled' rather than producing a
-- malformed or empty path segment.
--
-- `p_predicted_class` is client-supplied, so this is a defensive parse, not a
-- trusted value — the same discipline as the old (deleted) Android
-- TrainingDataRepository.sanitizeLabel, now enforced server-side instead.
-- ---------------------------------------------------------------------------
create or replace function sanitize_label(p_label text)
returns text
language sql
immutable
as $$
  select coalesce(
    nullif(
      trim(both '_' from
        regexp_replace(
          regexp_replace(lower(coalesce(p_label, '')), '[()]', '', 'g'),
          '[^a-z0-9]+', '_', 'g'
        )
      ),
      ''
    ),
    'unlabeled'
  );
$$;

-- ---------------------------------------------------------------------------
-- Step 6 of the pipeline: duplicate check *before* any bytes are accepted.
--
-- Returns jsonb:
--   { "accepted": true,  "duplicate": false, "storage_path": "contributions/<label>/<uuid>.jpg" }
--   { "accepted": false, "duplicate": true }
--
-- The folder is the label already confirmed or corrected by the app user in
-- the Verify screen before consenting — not a raw, unreviewed model guess,
-- and not the separate Phase 2 dermatologist double-review (that workflow
-- doesn't exist yet; see labels/dataset_versions in 01_schema.sql). If Phase 2
-- ships, re-foldering by the adjudicated label is a follow-up, not a rebuild
-- of this function.
--
-- Idempotent by id: calling it again for a contribution that is already
-- registered returns the same storage path rather than a duplicate verdict, so
-- a client retrying after a failed byte upload resumes instead of being
-- rejected by its own earlier attempt.
--
-- Withdrawn rows are excluded from the duplicate scan. A withdrawn image has
-- had its bytes deleted, so treating it as a blocker would permanently prevent
-- anyone else from contributing the same clinical presentation.
--
-- Sequential scan over `contributions` is intentional. At capstone volumes
-- (thousands of rows) this is microseconds; a BK-tree or a bit-sliced index is
-- only worth it past six figures.
-- ---------------------------------------------------------------------------
create or replace function register_contribution(
  p_id               uuid,
  p_phash            text,
  p_predicted_class  text,
  p_confidence       real,
  p_consent_version  text,
  p_app_version      text,
  p_contributed_date date,
  p_region           text default 'GH',
  p_max_distance     int  default 5
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_existing_path text;
  v_duplicate_id  uuid;
  v_path          text;
begin
  if p_phash !~ '^[0-9a-f]{16}$' then
    raise exception 'phash must be 16 lowercase hex characters, got %', p_phash
      using errcode = '22023';
  end if;

  -- Retry of a registration we already accepted.
  select storage_path into v_existing_path
    from contributions
   where id = p_id;

  if found then
    return jsonb_build_object(
      'accepted', true, 'duplicate', false, 'storage_path', v_existing_path
    );
  end if;

  select id into v_duplicate_id
    from contributions
   where status <> 'withdrawn'
     and phash_distance(phash, p_phash) <= p_max_distance
   limit 1;

  if found then
    return jsonb_build_object('accepted', false, 'duplicate', true);
  end if;

  v_path := 'contributions/' || sanitize_label(p_predicted_class) || '/' || p_id::text || '.jpg';

  insert into contributions (
    id, storage_path, phash, predicted_class, confidence,
    consent_version, app_version, contributed_date, region, status
  ) values (
    p_id, v_path, p_phash, p_predicted_class, p_confidence,
    p_consent_version, p_app_version, p_contributed_date,
    coalesce(p_region, 'GH'), 'pending'
  );

  return jsonb_build_object(
    'accepted', true, 'duplicate', false, 'storage_path', v_path
  );
end;
$$;

-- ---------------------------------------------------------------------------
-- Called after the client believes the bytes landed. Until this runs the row
-- is registered but has no image, and the labelling queue must ignore it.
--
-- Verifies the object actually exists in storage.objects before setting
-- stored_at, rather than trusting the client's word for it. This is not
-- defensive-for-its-own-sake: it was added after finding rows with stored_at
-- set and zero corresponding bytes anywhere in the bucket, because the client
-- can observe an HTTP response that looks like success (including a 409 the
-- client treats as "already there") without the storage backend actually
-- having written anything. Raising here — instead of silently no-op'ing —
-- turns that into a normal Retry on the client rather than a row that lies
-- about having an image forever.
-- ---------------------------------------------------------------------------
create or replace function mark_contribution_stored(p_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_path   text;
  v_exists boolean;
begin
  select storage_path into v_path from contributions where id = p_id;
  if not found then
    raise exception 'No contribution registered for id %', p_id
      using errcode = '22023';
  end if;

  select exists(
    select 1 from storage.objects
     where bucket_id = 'scans' and name = v_path
  ) into v_exists;

  if not v_exists then
    raise exception 'No object found in storage at % for contribution %', v_path, p_id
      using errcode = '22023';
  end if;

  update contributions
     set stored_at = now()
   where id = p_id
     and stored_at is null;
end;
$$;

-- ---------------------------------------------------------------------------
-- Class-aware prioritisation (spec 3.4). Counts only images that are actually
-- usable as training data. Exposes no image, no path and no hash.
-- ---------------------------------------------------------------------------
create or replace function curated_class_counts()
returns jsonb
language sql
security definer
set search_path = public
as $$
  select coalesce(jsonb_object_agg(predicted_class, n), '{}'::jsonb)
    from (
      select predicted_class, count(*) as n
        from contributions
       where stored_at is not null
         and status in ('pending', 'labelled')
       group by predicted_class
    ) counts;
$$;

-- ---------------------------------------------------------------------------
-- Execution grants. The app authenticates with the anon key, so `anon` needs
-- these; `authenticated` is granted too for when Supabase Auth is wired up
-- alongside Firebase.
--
-- Note what is NOT granted: no direct table privileges. Revoking them is what
-- stops a contributor enumerating the collection with the anon key.
-- ---------------------------------------------------------------------------
revoke all on function register_contribution(uuid, text, text, real, text, text, date, text, int) from public;
revoke all on function mark_contribution_stored(uuid) from public;
revoke all on function curated_class_counts() from public;

grant execute on function register_contribution(uuid, text, text, real, text, text, date, text, int) to anon, authenticated;
grant execute on function mark_contribution_stored(uuid) to anon, authenticated;
grant execute on function curated_class_counts() to anon, authenticated;
