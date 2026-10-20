-- Dermify curation pipeline — Row Level Security
--
-- Supabase denies everything by default once RLS is on. That is the correct
-- posture and it is also the cause of the open upload bug: with no INSERT
-- policy on storage.objects, uploads fail and the failure surfaces as an
-- unhelpful Postgres error rather than a clear "denied".
--
-- Read 04_diagnose.sql before running this if uploads are currently failing —
-- confirm the actual cause rather than assuming it was policies.

-- ---------------------------------------------------------------------------
-- contributions: no direct table access for the app at all.
--
-- Every client operation goes through the SECURITY DEFINER functions in
-- 02_functions.sql. With RLS on and no permissive policy, a contributor
-- holding the anon key cannot read, list, or modify anyone's rows — including
-- their own. That is acceptance criterion 4, enforced by the database rather
-- than by the client behaving well.
-- ---------------------------------------------------------------------------
alter table contributions enable row level security;
alter table labels enable row level security;
alter table dataset_versions enable row level security;
alter table dataset_version_members enable row level security;

revoke all on table contributions from anon, authenticated;
revoke all on table labels from anon, authenticated;
revoke all on table dataset_versions from anon, authenticated;
revoke all on table dataset_version_members from anon, authenticated;

-- Phase 2 will add SELECT/UPDATE policies for Tier 2 and Tier 3 here, once the
-- Firebase tier claim is available to Postgres. Until then, expert review runs
-- through the Supabase dashboard under service_role, which never leaves the
-- server.

-- ---------------------------------------------------------------------------
-- Storage: the `scans` bucket.
--
-- The bucket name is case-sensitive and must match SupabaseManager.BUCKET
-- exactly. Contributions are written under the `contributions/` prefix at a
-- server-assigned path derived from the contribution UUID.
-- ---------------------------------------------------------------------------
insert into storage.buckets (id, name, public)
values ('scans', 'scans', false)
on conflict (id) do nothing;

-- INSERT: contributors may write, but only under the contributions/ prefix.
drop policy if exists "dermify_contributions_insert" on storage.objects;
create policy "dermify_contributions_insert"
  on storage.objects for insert
  to anon, authenticated
  with check (
    bucket_id = 'scans'
    and (storage.foldername(name))[1] = 'contributions'
  );

-- UPDATE: needed because the client uploads with upsert = true, so that a
-- retry after a partially-transferred object overwrites rather than colliding.
-- Same prefix restriction applies.
drop policy if exists "dermify_contributions_update" on storage.objects;
create policy "dermify_contributions_update"
  on storage.objects for update
  to anon, authenticated
  using (
    bucket_id = 'scans'
    and (storage.foldername(name))[1] = 'contributions'
  )
  with check (
    bucket_id = 'scans'
    and (storage.foldername(name))[1] = 'contributions'
  );

-- No SELECT policy and no DELETE policy for anon/authenticated. A contributor
-- writes and cannot read back — not their own image and not anyone else's.
-- Reading images is Tier 2/3 work and happens through the dashboard or an Edge
-- Function running as service_role.
drop policy if exists "dermify_contributions_select" on storage.objects;
drop policy if exists "dermify_contributions_delete" on storage.objects;

-- ---------------------------------------------------------------------------
-- Withdrawal (spec 4.4) runs as service_role, because deletion is exactly the
-- privilege that must not ship in the app. Phase 3 calls this from an Edge
-- Function; it is defined here so the deletion semantics live with the schema.
-- ---------------------------------------------------------------------------
create or replace function withdraw_contribution(p_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  delete from storage.objects
   where bucket_id = 'scans'
     and name = (select storage_path from contributions where id = p_id);

  update contributions
     set status = 'withdrawn',
         stored_at = null
   where id = p_id;
end;
$$;

revoke all on function withdraw_contribution(uuid) from public, anon, authenticated;
