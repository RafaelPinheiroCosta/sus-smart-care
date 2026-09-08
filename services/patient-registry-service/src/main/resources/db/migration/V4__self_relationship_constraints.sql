create unique index uq_representative_active_self_patient
    on representative_relationships(patient_id)
    where active = true and type = 'SELF';

create unique index uq_representative_active_self_user
    on representative_relationships(representative_user_id)
    where active = true and type = 'SELF';