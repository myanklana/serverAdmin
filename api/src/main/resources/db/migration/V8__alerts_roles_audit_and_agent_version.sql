alter table app_users add column role varchar(20) not null default 'ADMIN';
alter table servers add column agent_version varchar(40);

create table alert_rules (
    id uuid primary key,
    server_id uuid not null references servers(id) on delete cascade,
    created_by uuid not null references app_users(id) on delete cascade,
    metric varchar(20) not null,
    threshold double precision not null,
    enabled boolean not null,
    cooldown_seconds integer not null,
    last_triggered_at timestamp with time zone
);
create table alert_events (
    id uuid primary key,
    rule_id uuid not null references alert_rules(id) on delete cascade,
    server_id uuid not null references servers(id) on delete cascade,
    metric_value double precision not null,
    threshold double precision not null,
    created_at timestamp with time zone not null,
    acknowledged_at timestamp with time zone
);
create index idx_alert_rules_server on alert_rules(server_id, enabled);
create index idx_alert_events_created on alert_events(server_id, created_at);

create table audit_logs (
    id uuid primary key,
    user_id uuid references app_users(id) on delete set null,
    username varchar(80) not null,
    action varchar(80) not null,
    server_id uuid references servers(id) on delete set null,
    source_ip varchar(64),
    details text,
    created_at timestamp with time zone not null
);
create index idx_audit_logs_created on audit_logs(created_at);
