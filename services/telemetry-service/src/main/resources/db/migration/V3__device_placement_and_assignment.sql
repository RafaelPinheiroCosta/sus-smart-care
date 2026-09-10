alter table medical_devices
  add column updated_at timestamptz;

update medical_devices
set updated_at = registered_at
where updated_at is null;

alter table medical_devices
  alter column updated_at set not null;

create table device_placements (
  id uuid primary key,
  device_id uuid not null,
  placement_type varchar(30) not null,
  facility_id uuid,
  zone_id uuid,
  ambulance_id varchar(100),
  started_at timestamptz not null,
  ended_at timestamptz,

  constraint fk_device_placement_device
    foreign key (device_id)
    references medical_devices(id),

  constraint ck_device_placement_type
    check (placement_type in ('FACILITY','CARE_ZONE','AMBULANCE')),

  constraint ck_device_placement_reference
    check (
      (
        placement_type = 'FACILITY'
        and facility_id is not null
        and zone_id is null
        and ambulance_id is null
      )
      or
      (
        placement_type = 'CARE_ZONE'
        and facility_id is not null
        and zone_id is not null
        and ambulance_id is null
      )
      or
      (
        placement_type = 'AMBULANCE'
        and facility_id is null
        and zone_id is null
        and ambulance_id is not null
      )
    )
);

create unique index uq_device_active_placement
  on device_placements(device_id)
  where ended_at is null;

create table device_assignments (
  id uuid primary key,
  device_id uuid not null,
  telemetry_session_id uuid not null,
  started_at timestamptz not null,
  ended_at timestamptz,

  constraint fk_device_assignment_device
    foreign key (device_id)
    references medical_devices(id),

  constraint fk_device_assignment_session
    foreign key (telemetry_session_id)
    references telemetry_sessions(id)
);

create unique index uq_device_active_assignment
  on device_assignments(device_id)
  where ended_at is null;

create index idx_device_assignment_session
  on device_assignments(telemetry_session_id, ended_at);

create index idx_device_placement_facility
  on device_placements(facility_id, zone_id)
  where ended_at is null;

create index idx_device_placement_ambulance
  on device_placements(ambulance_id)
  where ended_at is null;
