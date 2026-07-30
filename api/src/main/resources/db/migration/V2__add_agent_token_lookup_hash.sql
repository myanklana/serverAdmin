ALTER TABLE servers ADD COLUMN token_lookup_hash VARCHAR(64);
UPDATE servers SET token_lookup_hash = CAST(id AS VARCHAR) WHERE token_lookup_hash IS NULL;
ALTER TABLE servers ALTER COLUMN token_lookup_hash SET NOT NULL;
ALTER TABLE servers ADD CONSTRAINT uk_server_token_lookup_hash UNIQUE (token_lookup_hash);
