create table presence_gateways (
  id uuid primary key,
  external_id varchar(100) not null unique,
  source_type varchar(20) not null,
  facility_id uuid not null,
  zone_id varchar(100) not null,
  active boolean not null,
  created_at timestamptz not null,

  constraint ck_presence_gateway_source
    check (
      source_type in (
        'BLE',
        'WIFI',
        'UWB',
        'QR',
        'KIOSK'
      )
    )
);

create table presence_tracking_sessions (
  id uuid primary key,
  visit_id uuid not null,
  patient_id uuid not null,
  facility_id uuid not null,
  tracking_token_hash varchar(64) not null unique,
  inside_facility boolean not null default false,
  current_zone_id varchar(100),
  zone_entered_at timestamptz,
  last_signal_at timestamptz,
  last_source_type varchar(20),
  last_gateway_external_id varchar(100),
  started_at timestamptz not null,
  ended_at timestamptz,
  version bigint not null default 0,

  constraint ck_presence_tracking_source
    check (
      last_source_type is null or
      last_source_type in (
        'BLE',
        'WIFI',
        'UWB',
        'QR',
        'KIOSK',
        'MANUAL',
        'SYSTEM'
      )
    )
);

create unique index uq_presence_active_tracking_visit
  on presence_tracking_sessions(visit_id)
  where ended_at is null;

create index idx_presence_tracking_zone
  on presence_tracking_sessions(
    facility_id,
    current_zone_id
  )
  where
    ended_at is null and
    inside_facility = true;

create table presence_signals (
  id uuid primary key,
  tracking_session_id uuid not null,
  source_type varchar(20) not null,
  presence_state varchar(20) not null,
  zone_id varchar(100),
  gateway_external_id varchar(100),
  occurred_at timestamptz not null,
  received_at timestamptz not null,

  constraint fk_presence_signal_session
    foreign key (tracking_session_id)
    references presence_tracking_sessions(id),

  constraint ck_presence_signal_source
    check (
      source_type in (
        'BLE',
        'WIFI',
        'UWB',
        'QR',
        'KIOSK',
        'MANUAL',
        'SYSTEM'
      )
    ),

  constraint ck_presence_signal_state
    check (
      presence_state in (
        'INSIDE',
        'OUTSIDE'
      )
    )
);

create index idx_presence_signal_session_time
  on presence_signals(
    tracking_session_id,
    occurred_at
  );
