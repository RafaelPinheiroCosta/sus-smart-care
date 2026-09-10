create table mqtt_ingestion_messages (
  id uuid primary key,
  device_id uuid not null,
  message_id uuid not null,
  sequence_number bigint not null,
  received_at timestamptz not null,

  constraint fk_mqtt_ingestion_device
    foreign key (device_id)
    references medical_devices(id),

  constraint ck_mqtt_ingestion_sequence
    check (sequence_number >= 0)
);

create unique index uq_mqtt_device_message
  on mqtt_ingestion_messages(
    device_id,
    message_id
  );

create unique index uq_mqtt_device_sequence
  on mqtt_ingestion_messages(
    device_id,
    sequence_number
  );

create index idx_mqtt_ingestion_received
  on mqtt_ingestion_messages(
    received_at
  );
