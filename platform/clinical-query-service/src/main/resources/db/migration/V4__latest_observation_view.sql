create table if not exists clinical_latest_observation_views (
    id uuid primary key,
    visit_id uuid not null,
    type varchar(120) not null,
    value double precision not null,
    unit varchar(40),
    measured_at timestamptz not null,
    constraint uk_clinical_latest_observation unique (visit_id, type)
);
create index if not exists idx_clinical_latest_observation_visit
    on clinical_latest_observation_views (visit_id, type);
