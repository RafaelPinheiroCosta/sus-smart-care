alter table clinical_patient_views
  add column if not exists journey_stage varchar(120);

alter table clinical_patient_views
  add column if not exists journey_outcome varchar(40);

alter table clinical_patient_views
  add column if not exists transfer_facility_id uuid;

alter table clinical_patient_views
  add column if not exists terminal_note varchar(1000);

alter table clinical_patient_views
  add column if not exists latest_aggregate_type varchar(120);

alter table clinical_patient_views
  add column if not exists latest_aggregate_count bigint;

alter table clinical_patient_views
  add column if not exists latest_aggregate_minimum double precision;

alter table clinical_patient_views
  add column if not exists latest_aggregate_maximum double precision;

alter table clinical_patient_views
  add column if not exists latest_aggregate_average double precision;

alter table clinical_patient_views
  add column if not exists latest_aggregate_unit varchar(40);

alter table clinical_patient_views
  add column if not exists latest_aggregate_measured_at timestamptz;

alter table clinical_patient_views
  add column if not exists latest_anomaly_type varchar(120);

alter table clinical_patient_views
  add column if not exists latest_anomaly_value double precision;

alter table clinical_patient_views
  add column if not exists latest_anomaly_unit varchar(40);

alter table clinical_patient_views
  add column if not exists latest_anomaly_reason varchar(300);

alter table clinical_patient_views
  add column if not exists latest_anomaly_detected_at timestamptz;
