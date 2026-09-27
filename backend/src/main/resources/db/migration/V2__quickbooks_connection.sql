CREATE TABLE quickbooks_connections (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    realm_id            VARCHAR(50) NOT NULL UNIQUE,
    access_token        TEXT NOT NULL,
    refresh_token       TEXT NOT NULL,
    access_token_expires_at  TIMESTAMPTZ NOT NULL,
    refresh_token_expires_at TIMESTAMPTZ NOT NULL,
    connected_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_synced_at      TIMESTAMPTZ
);
