-- =============================================================
-- V13: Physician credentialing for Canadian College-registration verification.
--
-- Canadian physicians are licensed by a provincial College (CPSO in Ontario, CMQ in
-- Quebec, CPSBC in BC, CPSA in Alberta, ...). A platform must capture the College + the
-- registration number and confirm the physician is registered "in good standing" before
-- they act clinically. These columns hold those credentials and the verification state;
-- the live registry lookup is a pluggable adapter (real College API + manual-review
-- fallback), so the schema is the same whether verification is automated or manual.
-- =============================================================

ALTER TABLE clinic_staff
    ADD COLUMN college             VARCHAR(50),
    ADD COLUMN registration_number VARCHAR(50),
    ADD COLUMN license_province    VARCHAR(2),
    ADD COLUMN billing_number      VARCHAR(50),
    ADD COLUMN verification_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED'
        CHECK (verification_status IN ('UNVERIFIED','PENDING','VERIFIED','REJECTED')),
    ADD COLUMN verification_source VARCHAR(50),
    ADD COLUMN verified_at         TIMESTAMPTZ;

-- Find unverified / pending physicians quickly for an admin review queue.
CREATE INDEX idx_clinic_staff_verification_status ON clinic_staff(verification_status);
