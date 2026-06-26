-- WEARABLE_DATA consent: explicit consent to connect a wearable / device health source and share
-- the readings with the patient's clinic. Required before any wearable data is ingested or shown.
-- The consent_type check constraint is an explicit whitelist (see V6, which added AI_TRAINING), so
-- a new consent type must be added here too. patient_consents stays append-only.

ALTER TABLE patient_consents DROP CONSTRAINT patient_consents_consent_type_check;

ALTER TABLE patient_consents ADD CONSTRAINT patient_consents_consent_type_check
    CHECK (consent_type IN ('DATA_PROCESSING', 'AI_ANALYSIS', 'PHI_SHARING', 'MARKETING', 'AI_TRAINING', 'WEARABLE_DATA'));
