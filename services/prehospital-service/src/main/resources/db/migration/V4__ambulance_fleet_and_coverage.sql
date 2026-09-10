create table ambulances (
  id varchar(100) primary key,
  display_name varchar(160) not null,
  operational_status varchar(40) not null,
  created_at timestamptz not null,
  updated_at timestamptz not null
);

insert into ambulances (
  id,
  display_name,
  operational_status,
  created_at,
  updated_at
)
select
  ambulance_id,
  ambulance_id,
  'ACTIVE',
  min(created_at),
  min(created_at)
from pre_hospital_encounters
group by ambulance_id
on conflict (id) do nothing;

alter table pre_hospital_encounters
  add constraint fk_pre_hospital_encounter_ambulance
  foreign key (ambulance_id)
  references ambulances(id);

create table ambulance_coverages (
  id uuid primary key,
  ambulance_id varchar(100) not null,
  facility_id uuid not null,
  started_at timestamptz not null,
  ended_at timestamptz,

  constraint fk_ambulance_coverage_ambulance
    foreign key (ambulance_id)
    references ambulances(id)
);

create unique index uq_ambulance_active_coverage
  on ambulance_coverages(ambulance_id)
  where ended_at is null;

create index idx_ambulance_active_coverage_facility
  on ambulance_coverages(facility_id)
  where ended_at is null;
