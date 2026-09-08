create table patients(id uuid primary key, full_name varchar(200) not null, birth_date date, identity_status varchar(30) not null, created_at timestamptz not null, merged_into_patient_id uuid);
create table patient_identifiers(id uuid primary key, patient_id uuid not null, type varchar(30) not null, value varchar(120) not null, unique(type,value));
create table representative_relationships(id uuid primary key, patient_id uuid not null, representative_user_id uuid not null, type varchar(40) not null, valid_from timestamptz not null, valid_until timestamptz, active boolean not null);
create index idx_rep_patient_active on representative_relationships(patient_id,active);
