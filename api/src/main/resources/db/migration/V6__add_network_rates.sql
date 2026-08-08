ALTER TABLE metrics
    ADD COLUMN network_received_bytes_per_second BIGINT NOT NULL DEFAULT 0;

ALTER TABLE metrics
    ADD COLUMN network_sent_bytes_per_second BIGINT NOT NULL DEFAULT 0;
