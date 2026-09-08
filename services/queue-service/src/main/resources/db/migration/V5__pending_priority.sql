create table if not exists pending_queue_priorities(visit_id uuid primary key,priority varchar(30) not null,received_at timestamptz not null);
