-- Dermify — diagnosing the failing uploads (project context §9, spec §4.1)
--
-- Symptom: images are not reaching the `scans` bucket; Postgres logs show
-- 58P01 (undefined_file).
--
-- Run these in order in the Supabase SQL editor and record the actual output.
-- The point is to find out which of the three candidates it is, not to apply
-- all three fixes and declare victory — the project has been burned twice by
-- trusting a description over a measurement.

-- ---------------------------------------------------------------------------
-- 1. Does the bucket exist, and is the name an exact match?
--
-- Expect exactly one row with id = 'scans'. A row named 'Scans' or 'scan' is
-- the bug: the name is case-sensitive and the client sends the literal in
-- SupabaseManager.BUCKET.
-- ---------------------------------------------------------------------------
select id, name, public, created_at
  from storage.buckets;

-- ---------------------------------------------------------------------------
-- 2. Is there an INSERT policy for the role the app actually authenticates as?
--
-- The app ships the anon key and does not sign into Supabase Auth — it uses
-- Firebase for identity. So the role at the database is `anon`, NOT
-- `authenticated`. A policy written `to authenticated` looks correct in the
-- dashboard and denies every upload the app makes.
--
-- Expect the dermify_contributions_insert policy from 03_policies.sql with
-- roles including anon.
-- ---------------------------------------------------------------------------
select policyname, cmd, roles, qual, with_check
  from pg_policies
 where schemaname = 'storage'
   and tablename = 'objects';

-- ---------------------------------------------------------------------------
-- 3. Is RLS actually enabled on storage.objects?
-- ---------------------------------------------------------------------------
select relname, relrowsecurity
  from pg_class
 where relname = 'objects'
   and relnamespace = 'storage'::regnamespace;

-- ---------------------------------------------------------------------------
-- 4. Simulate the app's upload as the anon role.
--
-- This is the measurement that settles it. If this insert succeeds, the
-- database side is fine and the failure is client-side (auth state, bucket
-- string, or the request never being sent). If it raises, the message names
-- the real cause.
-- ---------------------------------------------------------------------------
begin;
  set local role anon;
  insert into storage.objects (bucket_id, name, owner, metadata)
  values ('scans', 'contributions/00000000-0000-0000-0000-000000000000.jpg', null, '{}'::jsonb);
rollback;

-- ---------------------------------------------------------------------------
-- 5. What has actually arrived?
--
-- Registered rows with stored_at null mean register_contribution succeeded but
-- the byte upload did not — which points at storage policies, not at the
-- table. Rows present with stored_at set mean the pipeline works end to end.
-- ---------------------------------------------------------------------------
select count(*) filter (where stored_at is null)     as registered_no_bytes,
       count(*) filter (where stored_at is not null) as stored,
       count(*)                                      as total
  from contributions;

select name, created_at, (metadata->>'size')::bigint as bytes
  from storage.objects
 where bucket_id = 'scans'
 order by created_at desc
 limit 20;

-- ---------------------------------------------------------------------------
-- 6. Client side, in parallel with the above.
--
-- ContributionUploader now logs the full exception rather than just
-- e.message, which is why this was previously invisible — the status code and
-- response body were being dropped. Watch for it with:
--
--   adb logcat -s ContributionUpload:* ContributionRepo:* Deidentify:*
--
-- and read `lastError` on the outbox row, which the Profile screen surfaces.
-- ---------------------------------------------------------------------------
