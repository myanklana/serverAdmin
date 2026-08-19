CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE servers (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    hostname VARCHAR(255) NOT NULL,
    ip VARCHAR(45) NOT NULL,
    port INTEGER NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    last_seen TIMESTAMP WITH TIME ZONE,
    owner_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    CONSTRAINT uk_server_owner_name UNIQUE (owner_id, name),
    CONSTRAINT ck_server_port CHECK (port BETWEEN 1 AND 65535)
);
