CREATE TABLE auth_rate_limits (
    id UUID PRIMARY KEY,
    scope VARCHAR(32) NOT NULL,
    ip_hash VARCHAR(64) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    window_started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    blocked_until TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_auth_rate_limits_scope_ip UNIQUE (scope, ip_hash)
);
