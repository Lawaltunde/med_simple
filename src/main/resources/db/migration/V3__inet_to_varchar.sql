-- Hibernate 7 / JDBC does not support PostgreSQL inet type natively.
-- IP addresses stored as varchar(45) — covers IPv4 (15 chars) and IPv6 (39 chars).
-- Validation happens at the application layer.
ALTER TABLE audit_logs ALTER COLUMN ip_address TYPE varchar(45);
ALTER TABLE patient_consents ALTER COLUMN ip_address TYPE varchar(45);
