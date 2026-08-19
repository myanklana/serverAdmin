create table agent_commands (
    id uuid primary key,
    server_id uuid not null references servers(id),
    requested_by uuid not null references app_users(id),
    command_type varchar(40) not null,
    payload text,
    status varchar(30) not null,
    created_at timestamp with time zone not null,
    claimed_at timestamp with time zone,
    completed_at timestamp with time zone,
    result text,
    error_message text
);

create index idx_agent_commands_pending
    on agent_commands(server_id, status, created_at);