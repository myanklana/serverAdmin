CREATE TABLE metrics (
    id UUID PRIMARY KEY, server_id UUID NOT NULL REFERENCES servers(id) ON DELETE CASCADE, collected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    cpu_percent DOUBLE PRECISION NOT NULL, memory_used_bytes BIGINT NOT NULL, memory_total_bytes BIGINT NOT NULL,
    disk_used_bytes BIGINT NOT NULL, disk_total_bytes BIGINT NOT NULL, operating_system VARCHAR(255) NOT NULL,
    kernel VARCHAR(255) NOT NULL, architecture VARCHAR(100) NOT NULL, uptime_seconds BIGINT NOT NULL,
    network_received_bytes BIGINT NOT NULL, network_sent_bytes BIGINT NOT NULL
);
CREATE INDEX idx_metrics_server_collected_at ON metrics(server_id, collected_at DESC);
