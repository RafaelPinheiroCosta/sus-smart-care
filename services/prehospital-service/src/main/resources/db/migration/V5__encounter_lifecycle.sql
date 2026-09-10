alter table pre_hospital_encounters
  add column arrived_at timestamptz;

alter table pre_hospital_encounters
  add column cancelled_at timestamptz;

alter table pre_hospital_encounters
  add column cancellation_reason varchar(500);

alter table pre_hospital_encounters
  add constraint ck_pre_hospital_encounter_status
  check (status in ('EN_ROUTE', 'ARRIVED', 'CANCELLED'));

create index idx_pre_hospital_en_route_destination
  on pre_hospital_encounters(
    destination_facility_id,
    estimated_arrival_at
  )
  where status = 'EN_ROUTE';
