-- =============================================================
-- V14: Wearable / device readings (patient-generated health data).
--
-- Consumer wearables and phones (heart rate, steps, sleep, SpO2) feed the platform as FHIR
-- Observations. This is patient-generated health data (PGHD): consumer-grade, NOT a validated
-- clinical measurement. Every row therefore records its source/provenance and the device label,
-- so the physician always sees it labeled as such and never mistakes a consumer watch reading for
-- a clinical-grade one.
--
-- The data is informational only: it is displayed and summarized for the physician, and the
-- platform never makes an autonomous clinical determination or alert on it (active vitals
-- monitoring with automated escalation is a regulated device function, sequenced to Phase 3).
--
-- Ingestion is a pluggable adapter (real wearable aggregator + synthetic/manual fallback), so the
-- schema is identical whether a reading arrives from a device cloud, a webhook, or manual entry.
-- =============================================================

CREATE TABLE wearable_readings (
    id            UUID PRIMARY KEY,
    patient_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    metric        VARCHAR(30) NOT NULL
        CHECK (metric IN ('HEART_RATE','RESTING_HEART_RATE','STEPS','SLEEP_MINUTES','SPO2')),
    value         DOUBLE PRECISION NOT NULL,
    unit          VARCHAR(20) NOT NULL,
    effective_at  TIMESTAMPTZ NOT NULL,          -- when the measurement was taken
    source        VARCHAR(20) NOT NULL
        CHECK (source IN ('MANUAL','SYNTHETIC','AGGREGATOR')),
    device_label  VARCHAR(80),                   -- e.g. "Apple Watch", "Manual entry"
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Trend / series reads are always per patient + metric, newest first.
CREATE INDEX idx_wearable_readings_patient_metric_time
    ON wearable_readings(patient_id, metric, effective_at DESC);
