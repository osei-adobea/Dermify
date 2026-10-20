-- Dermify curation pipeline — schema
-- Run in the Supabase SQL editor, in file-number order.
--
-- Phase 1 needs `contributions` only. `labels`, `dataset_versions` and
-- `dataset_version_members` are created here so the schema is one artefact,
-- but nothing writes to them until Phase 2.

create extension if not exists pgcrypto;

-- ---------------------------------------------------------------------------
-- One row per contributed image.
--
-- Deliberately absent: user id, device id, Firebase UID, original filename,
-- any timestamp finer than a date on the contribution itself. `created_at`
-- records when the server saw the row, which is server-side bookkeeping and
-- not tied to the contributor.
-- ---------------------------------------------------------------------------
create table if not exists contributions (
  id                uuid primary key,
  storage_path      text not null,
  phash             text not null,          -- 16 lowercase hex chars (64-bit pHash)
  predicted_class   text not null,          -- the model's guess, never ground truth
  confidence        real not null,
  consent_version   text not null,
  app_version       text not null,
  contributed_date  date not null,
  region            text default 'GH',      -- country only, never GPS
  status            text not null default 'pending',
    -- pending | labelled | disputed | rejected | withdrawn
  -- Set once the image bytes actually land in storage. Registration reserves
  -- the row before the upload, so without this the labelling queue would show
  -- rows whose image never arrived.
  stored_at         timestamptz,
  created_at        timestamptz default now(),

  constraint contributions_phash_format check (phash ~ '^[0-9a-f]{16}$'),
  constraint contributions_status_valid check (
    status in ('pending', 'labelled', 'disputed', 'rejected', 'withdrawn')
  )
);

create index if not exists contributions_phash_idx on contributions (phash);
create index if not exists contributions_status_idx on contributions (status);

-- ---------------------------------------------------------------------------
-- Phase 2 tables.
-- ---------------------------------------------------------------------------
create table if not exists labels (
  id              uuid primary key default gen_random_uuid(),
  contribution_id uuid references contributions(id) on delete cascade,
  labeller_id     uuid not null,            -- Firebase UID of a Tier 2/3 user
  label           text not null,            -- one of the four classes, or 'other' / 'unusable'
  confidence      text,                     -- certain | probable | uncertain
  notes           text,
  created_at      timestamptz default now(),
  unique (contribution_id, labeller_id)     -- one label per expert per image
);

create table if not exists dataset_versions (
  id            serial primary key,
  version_tag   text unique not null,       -- e.g. 'dermify-v2-2026-11'
  created_at    timestamptz default now(),
  image_count   int not null,
  class_counts  jsonb not null,
  notes         text
);

create table if not exists dataset_version_members (
  version_id      int references dataset_versions(id),
  contribution_id uuid references contributions(id),
  final_label     text not null,
  primary key (version_id, contribution_id)
);
