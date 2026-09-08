create table if not exists visit_events(id uuid primary key,stream_id uuid not null,event_version bigint not null,event_type varchar(120) not null,payload varchar(16000) not null,occurred_at timestamptz not null,constraint uk_visit_stream_version unique(stream_id,event_version));
create index if not exists idx_visit_events_stream on visit_events(stream_id,event_version);
create table if not exists outbox_events(id uuid primary key,topic varchar(160) not null,event_key varchar(160) not null,payload varchar(16000) not null,created_at timestamptz not null,published_at timestamptz,attempts integer not null default 0,next_attempt_at timestamptz not null,last_error varchar(2000));
create index if not exists idx_journey_outbox_pending on outbox_events(published_at,next_attempt_at,created_at);
