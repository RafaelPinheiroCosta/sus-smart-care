alter table clinical_patient_views
  add column if not exists journey_occurred_at timestamptz;

create index if not exists idx_clinical_patient_views_patient
  on clinical_patient_views(patient_id);
