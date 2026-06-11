-- =============================================================
-- MedSimple — Audit Log Immutability Enforcement
-- V2__audit_log_immutability.sql
--
-- PHIPA requires audit_logs to be an immutable, append-only ledger.
-- These triggers enforce that at the database layer so no application
-- bug or direct SQL connection can silently corrupt the audit trail.
-- =============================================================

CREATE OR REPLACE FUNCTION audit_logs_immutable()
    RETURNS TRIGGER
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION
        'audit_logs is append-only: % on audit_logs is prohibited', TG_OP;
END;
$$;

CREATE TRIGGER trg_audit_logs_no_update
    BEFORE UPDATE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION audit_logs_immutable();

CREATE TRIGGER trg_audit_logs_no_delete
    BEFORE DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION audit_logs_immutable();
