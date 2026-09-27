CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE app_users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    organization    VARCHAR(255) NOT NULL,
    role            VARCHAR(50)  NOT NULL DEFAULT 'ANALYST',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE vendors (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    VARCHAR(255) NOT NULL,
    tax_id                  VARCHAR(50),
    email                   VARCHAR(255),
    phone                   VARCHAR(50),
    bank_account_last4      VARCHAR(4),
    bank_routing_number     VARCHAR(20),
    bank_account_updated_at TIMESTAMPTZ,
    risk_score              NUMERIC(5,2) NOT NULL DEFAULT 0,
    is_verified             BOOLEAN NOT NULL DEFAULT FALSE,
    source                  VARCHAR(30) NOT NULL DEFAULT 'SYNTHETIC',
    external_id             VARCHAR(100),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_vendors_name ON vendors (name);
CREATE INDEX idx_vendors_external_id ON vendors (external_id);

CREATE TABLE bank_account_change_events (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_id           UUID NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
    old_account_last4   VARCHAR(4),
    old_routing_number  VARCHAR(20),
    new_account_last4   VARCHAR(4),
    new_routing_number  VARCHAR(20),
    changed_via         VARCHAR(50) NOT NULL DEFAULT 'EMAIL_REQUEST',
    detected_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_bank_change_vendor ON bank_account_change_events (vendor_id);

CREATE TABLE invoices (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_id           UUID NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
    invoice_number      VARCHAR(100) NOT NULL,
    amount              NUMERIC(14,2) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'USD',
    issued_date         DATE NOT NULL,
    due_date            DATE,
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    invoice_hash        VARCHAR(64) NOT NULL,
    source              VARCHAR(30) NOT NULL DEFAULT 'SYNTHETIC',
    external_id         VARCHAR(100),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_invoices_vendor ON invoices (vendor_id);
CREATE INDEX idx_invoices_hash ON invoices (invoice_hash);
CREATE INDEX idx_invoices_issued_date ON invoices (issued_date);

CREATE TABLE payments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id          UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    amount              NUMERIC(14,2) NOT NULL,
    paid_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    destination_account_last4  VARCHAR(4),
    destination_routing_number VARCHAR(20),
    status              VARCHAR(30) NOT NULL DEFAULT 'COMPLETED'
);

CREATE INDEX idx_payments_invoice ON payments (invoice_id);

CREATE TABLE fraud_alerts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_id           UUID NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
    invoice_id          UUID REFERENCES invoices(id) ON DELETE CASCADE,
    alert_type          VARCHAR(50) NOT NULL,
    severity            VARCHAR(20) NOT NULL,
    risk_score          NUMERIC(5,2) NOT NULL,
    reason              TEXT NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at         TIMESTAMPTZ,
    resolved_by         VARCHAR(255)
);

CREATE INDEX idx_alerts_vendor ON fraud_alerts (vendor_id);
CREATE INDEX idx_alerts_status ON fraud_alerts (status);
CREATE INDEX idx_alerts_created_at ON fraud_alerts (created_at);
