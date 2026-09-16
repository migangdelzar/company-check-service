CREATE TABLE verifications (
    id UUID PRIMARY KEY,
    raw_query TEXT NOT NULL,
    normalized_query TEXT NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL,
    state_version INTEGER NOT NULL DEFAULT 1,
    state JSONB NOT NULL,
    claim_token UUID,
    claimed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT verifications_status_ck CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED')),
    CONSTRAINT verifications_expiry_ck CHECK (expires_at > started_at),
    CONSTRAINT verifications_state_version_ck CHECK (state_version = 1)
);
CREATE INDEX verifications_expiry_idx ON verifications (expires_at) WHERE status = 'IN_PROGRESS';
CREATE INDEX verifications_normalized_query_idx ON verifications (normalized_query);
