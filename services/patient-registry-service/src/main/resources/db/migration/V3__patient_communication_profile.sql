create table patient_communication_profiles (
  patient_id uuid primary key,
  has_smartphone boolean not null,
  queue_call_mode varchar(40) not null,
  updated_at timestamptz not null,
  constraint fk_patient_communication_profile_patient
    foreign key (patient_id) references patients(id)
);