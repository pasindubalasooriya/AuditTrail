CREATE OR REPLACE FUNCTION prevent_audit_event_modification()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        RAISE EXCEPTION
            'Audit log is immutable. UPDATE not permitted on audit_events. Event ID: %', OLD.id;
    END IF;
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION
            'Audit log is immutable. DELETE not permitted on audit_events. Event ID: %', OLD.id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_events_immutable
    BEFORE UPDATE OR DELETE
    ON audit_events
    FOR EACH ROW
    EXECUTE FUNCTION prevent_audit_event_modification();

COMMENT ON FUNCTION prevent_audit_event_modification() IS
    'Enforces append-only immutability on audit_events. Required for financial compliance.';
