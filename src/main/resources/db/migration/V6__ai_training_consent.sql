-- AI_TRAINING consent: explicit, separate consent for using a patient's data to improve the AI
-- (a secondary use under PHIPA, distinct from delivering care). Default state is "no consent"
-- until a patient grants it; nothing enters a learning / RAG corpus without an active grant.
-- patient_consents stays append-only (each grant/revoke is a new row).

ALTER TABLE patient_consents DROP CONSTRAINT patient_consents_consent_type_check;

ALTER TABLE patient_consents ADD CONSTRAINT patient_consents_consent_type_check
    CHECK (consent_type IN ('DATA_PROCESSING', 'AI_ANALYSIS', 'PHI_SHARING', 'MARKETING', 'AI_TRAINING'));
