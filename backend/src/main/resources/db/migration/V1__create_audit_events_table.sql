CREATE TABLE IF NOT EXISTS audit_events (
    id               BIGSERIAL PRIMARY KEY,
    action_type      VARCHAR(50)   NOT NULL,
    performed_by     VARCHAR(255)  NOT NULL,
    performed_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    amount           DECIMAL(19,2),
    merchant_id      VARCHAR(100),
    transaction_id   VARCHAR(100),
    risk_level       VARCHAR(20),
    notes            TEXT,
    ip_address       VARCHAR(45)
);

CREATE INDEX idx_audit_events_performed_by ON audit_events(performed_by);
CREATE INDEX idx_audit_events_action_type  ON audit_events(action_type);
CREATE INDEX idx_audit_events_performed_at ON audit_events(performed_at DESC);
CREATE INDEX idx_audit_events_merchant_id  ON audit_events(merchant_id);

COMMENT ON TABLE audit_events IS 'Immutable audit log for all payment gateway actions';
