alter table telemetry_sessions
  add column mode varchar(20);

update telemetry_sessions
set mode = 'SPOT'
where mode is null;

alter table telemetry_sessions
  alter column mode set not null;

alter table telemetry_sessions
  add constraint ck_telemetry_session_mode
  check (mode in ('SPOT','CONTINUOUS'));

create table telemetry_aggregates (
  id uuid primary key,
  session_id uuid not null,
  metric_type varchar(120) not null,
  sample_count bigint not null,
  minimum_value double precision not null,
  maximum_value double precision not null,
  average_value double precision not null,
  unit varchar(40),
  last_measured_at timestamptz not null,
  updated_at timestamptz not null,

  constraint fk_telemetry_aggregate_session
    foreign key (session_id)
    references telemetry_sessions(id),

  constraint ck_telemetry_aggregate_count
    check (sample_count > 0)
);

create unique index uq_telemetry_aggregate_session_metric
  on telemetry_aggregates(
    session_id,
    metric_type
  );

create table telemetry_anomalies (
  id uuid primary key,
  session_id uuid not null,
  device_id uuid not null,
  metric_type varchar(120) not null,
  value double precision not null,
  unit varchar(40),
  measured_at timestamptz not null,
  detected_at timestamptz not null,
  reason varchar(300) not null,

  constraint fk_telemetry_anomaly_session
    foreign key (session_id)
    references telemetry_sessions(id),

  constraint fk_telemetry_anomaly_device
    foreign key (device_id)
    references medical_devices(id)
);

create index idx_telemetry_anomaly_session
  on telemetry_anomalies(
    session_id,
    detected_at desc
  );
