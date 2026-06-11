-- =============================================================
-- MedSimple — Initial Schema
-- V1__init_schema.sql
--
-- FHIR R4 resource mapping noted on each clinical table.
-- All timestamps: TIMESTAMPTZ (UTC stored, timezone-aware).
-- Health card and insurance numbers encrypted at application layer.
-- audit_logs is append-only — never UPDATE or DELETE rows.
-- Auth token tables added in V2__auth_schema.sql.
-- =============================================================

-- gen_random_uuid() is a core function from PostgreSQL 13+.
-- pgcrypto enabled defensively for compatibility with older environments.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";


-- =============================================================
-- CLINIC (TENANT ROOT)
-- =============================================================

CREATE TABLE clinics (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(255) NOT NULL,
    type          VARCHAR(50)  NOT NULL
                      CHECK (type IN ('FAMILY_PRACTICE','WALK_IN','VIRTUAL_CARE','FAMILY_HEALTH_TEAM','SPECIALIST')),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city          VARCHAR(100),
    province      VARCHAR(50),
    postal_code   VARCHAR(10),
    phone         VARCHAR(20),
    fax           VARCHAR(20),
    email         VARCHAR(255),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Per-clinic adapter registry.
-- When enabled, the real adapter fires; otherwise the built-in fallback fires.
CREATE TABLE clinic_capabilities (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id   UUID         NOT NULL REFERENCES clinics(id) ON DELETE CASCADE,
    capability  VARCHAR(100) NOT NULL
                    CHECK (capability IN (
                        'PRESCRIBIT',
                        'OCEAN_EREFERRAL',
                        'EMR_FHIR',
                        'LAB_ELECTRONIC',
                        'LAB_BOOKING',
                        'PHARMACY_ORDERING'
                    )),
    enabled     BOOLEAN      NOT NULL DEFAULT FALSE,
    config      JSONB,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (clinic_id, capability)
);


-- =============================================================
-- USERS
-- All actors: patients, physicians, nurses, admins.
-- Auth token tables (refresh, password reset) added in V2.
-- =============================================================

CREATE TABLE users (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    role           VARCHAR(50)  NOT NULL
                       CHECK (role IN ('PATIENT','PHYSICIAN','NURSE','RECEPTIONIST','CLINIC_ADMIN','SUPER_ADMIN')),
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    phone          VARCHAR(20),
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    email_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Maps physician/nurse/admin users to their clinic.
CREATE TABLE clinic_staff (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id      UUID        NOT NULL REFERENCES clinics(id) ON DELETE CASCADE,
    user_id        UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role           VARCHAR(50) NOT NULL
                       CHECK (role IN ('PHYSICIAN','NURSE','RECEPTIONIST','CLINIC_ADMIN')),
    license_number VARCHAR(100),
    specialization VARCHAR(100),
    active         BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (clinic_id, user_id)
);


-- =============================================================
-- EXTERNAL DIRECTORIES
-- =============================================================

CREATE TABLE pharmacies (
    id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name                   VARCHAR(255) NOT NULL,
    address_line1          VARCHAR(255),
    city                   VARCHAR(100),
    province               VARCHAR(50),
    postal_code            VARCHAR(10),
    phone                  VARCHAR(20),
    fax                    VARCHAR(20),
    supports_electronic_rx BOOLEAN      NOT NULL DEFAULT FALSE,
    active                 BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE diagnostic_centres (
    id                          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name                        VARCHAR(255) NOT NULL,
    address_line1               VARCHAR(255),
    city                        VARCHAR(100),
    province                    VARCHAR(50),
    postal_code                 VARCHAR(10),
    phone                       VARCHAR(20),
    fax                         VARCHAR(20),
    supports_electronic_booking BOOLEAN      NOT NULL DEFAULT FALSE,
    active                      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);


-- =============================================================
-- PATIENT PROFILE
-- Province intentionally omitted — residence address added via future migration.
-- Health coverage (including provincial card) lives in patient_health_coverage.
-- =============================================================

CREATE TABLE patient_profiles (
    id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               UUID        NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    date_of_birth         DATE,
    preferred_pharmacy_id UUID        REFERENCES pharmacies(id),
    -- [{name, severity}]
    allergies             JSONB       NOT NULL DEFAULT '[]'::jsonb,
    -- [{name, dose, frequency}]
    current_medications   JSONB       NOT NULL DEFAULT '[]'::jsonb,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Supports provincial health cards and private insurance simultaneously.
-- Province field = issuing province for the card, not patient residence.
-- card_number_enc: AES-256 encrypted at application layer, never stored plaintext.
CREATE TABLE patient_health_coverage (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id      UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    coverage_type   VARCHAR(50)  NOT NULL
                        CHECK (coverage_type IN ('PROVINCIAL','PRIVATE','VETERANS','NIHB')),
    provider_name   VARCHAR(100) NOT NULL,
    card_number_enc VARCHAR(500) NOT NULL,
    province        VARCHAR(50),
    expiry_date     DATE,
    is_primary      BOOLEAN      NOT NULL DEFAULT FALSE,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Explicit patient-clinic relationship.
-- Required for physician queue (who are my patients?) and
-- for routing new symptom submissions to the right clinic.
-- assigned_physician_id NULL for walk-in patients until episode is assigned.
CREATE TABLE patient_clinic_registrations (
    id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id            UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    clinic_id             UUID        NOT NULL REFERENCES clinics(id) ON DELETE CASCADE,
    assigned_physician_id UUID        REFERENCES users(id),
    registered_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    active                BOOLEAN     NOT NULL DEFAULT TRUE,
    UNIQUE (patient_id, clinic_id)
);

-- Versioned, immutable consent records (PHIPA requirement).
-- New event = new row; existing rows never updated.
CREATE TABLE patient_consents (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id   UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    consent_type VARCHAR(100) NOT NULL
                     CHECK (consent_type IN ('DATA_PROCESSING','AI_ANALYSIS','PHI_SHARING','MARKETING')),
    version      VARCHAR(20)  NOT NULL,
    granted_at   TIMESTAMPTZ,
    revoked_at   TIMESTAMPTZ,
    ip_address   INET,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);


-- =============================================================
-- SYMPTOM CAPTURE
-- FHIR: EpisodeOfCare (symptom_episodes) + Observation (symptom_submissions)
-- =============================================================

-- One episode per patient care complaint.
-- Accumulates submissions; holds AI-assessed and physician-confirmed urgency.
CREATE TABLE symptom_episodes (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id              UUID        NOT NULL REFERENCES users(id),
    assigned_clinic_id      UUID        REFERENCES clinics(id),
    assigned_physician_id   UUID        REFERENCES users(id),
    status                  VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED'
                                CHECK (status IN (
                                    'SUBMITTED',
                                    'AI_ANALYZED',
                                    'ASSIGNED',
                                    'IN_PROGRESS',
                                    'RESOLVED',
                                    'CLOSED',
                                    'EMERGENCY_DISPATCHED'
                                )),
    ai_urgency_level        VARCHAR(20)
                                CHECK (ai_urgency_level IN ('ROUTINE','SEE_SOON','URGENT','EMERGENCY')),
    ai_summary              TEXT,
    ai_analyzed_at          TIMESTAMPTZ,
    -- Physician confirms or overrides; this is the operative urgency for the queue
    confirmed_urgency_level VARCHAR(20)
                                CHECK (confirmed_urgency_level IN ('ROUTINE','SEE_SOON','URGENT','EMERGENCY')),
    urgency_overridden_by   UUID        REFERENCES users(id),
    urgency_override_reason TEXT,
    urgency_overridden_at   TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Individual symptom entries accumulating into an episode.
-- Voice and photo media stored in object storage (Supabase Storage); DB holds URL.
-- ai_analysis_result: AI's text output from photo or transcribed voice analysis.
CREATE TABLE symptom_submissions (
    id                 UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    episode_id         UUID        NOT NULL REFERENCES symptom_episodes(id) ON DELETE CASCADE,
    patient_id         UUID        NOT NULL REFERENCES users(id),
    input_type         VARCHAR(20) NOT NULL
                           CHECK (input_type IN ('TEXT','VOICE','PHOTO')),
    text_content       TEXT,
    media_url          VARCHAR(500),
    transcription      TEXT,
    ai_analysis_result TEXT,
    processing_status  VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                           CHECK (processing_status IN ('PENDING','PROCESSING','COMPLETED','FAILED')),
    submitted_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- =============================================================
-- CARE PLANS
-- FHIR: CarePlan
-- =============================================================

CREATE TABLE care_plans (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    episode_id   UUID        NOT NULL REFERENCES symptom_episodes(id),
    patient_id   UUID        NOT NULL REFERENCES users(id),
    physician_id UUID        NOT NULL REFERENCES users(id),
    clinic_id    UUID        NOT NULL REFERENCES clinics(id),
    status       VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'
                     CHECK (status IN ('ACTIVE','COMPLETED','CANCELLED')),
    notes        TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Bidirectional physician-patient messages within a care plan.
-- is_system = TRUE for AI-monitoring-generated prompts (symptom updates, deterioration alerts).
CREATE TABLE follow_ups (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    care_plan_id UUID        NOT NULL REFERENCES care_plans(id),
    initiated_by UUID        REFERENCES users(id),
    is_system    BOOLEAN     NOT NULL DEFAULT FALSE,
    message      TEXT        NOT NULL,
    response     TEXT,
    responded_by UUID        REFERENCES users(id),
    responded_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- =============================================================
-- PRESCRIPTIONS
-- FHIR: MedicationRequest
-- =============================================================

CREATE TABLE prescriptions (
    id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    care_plan_id             UUID         NOT NULL REFERENCES care_plans(id),
    patient_id               UUID         NOT NULL REFERENCES users(id),
    prescribing_physician_id UUID         NOT NULL REFERENCES users(id),
    medication_name          VARCHAR(255) NOT NULL,
    medication_code          VARCHAR(100),
    dosage                   VARCHAR(100) NOT NULL,
    frequency                VARCHAR(100) NOT NULL,
    duration                 VARCHAR(100),
    refills                  INTEGER      NOT NULL DEFAULT 0,
    instructions             TEXT,
    -- {interactions: [], allergyFlags: [], doseWarnings: []}
    ai_safety_checks         JSONB        NOT NULL DEFAULT '{}'::jsonb,
    status                   VARCHAR(50)  NOT NULL DEFAULT 'DRAFT'
                                 CHECK (status IN ('DRAFT','SIGNED','SENT_ELECTRONIC','SENT_PDF','FILLED','CANCELLED')),
    delivery_method          VARCHAR(50)
                                 CHECK (delivery_method IN ('PRESCRIBIT','PDF_FAX','PDF_PRINT')),
    pharmacy_id              UUID         REFERENCES pharmacies(id),
    -- Signed PDF URL in object storage (fallback path)
    document_url             VARCHAR(500),
    signed_at                TIMESTAMPTZ,
    sent_at                  TIMESTAMPTZ,
    filled_at                TIMESTAMPTZ,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);


-- =============================================================
-- REFERRALS — AI-assisted specialty referral triage
-- FHIR: ServiceRequest (intent = referral)
-- =============================================================

CREATE TABLE referrals (
    id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    care_plan_id           UUID         NOT NULL REFERENCES care_plans(id),
    patient_id             UUID         NOT NULL REFERENCES users(id),
    referring_physician_id UUID         NOT NULL REFERENCES users(id),
    ai_suggested_specialty VARCHAR(100) NOT NULL,
    confirmed_specialty    VARCHAR(100),
    clinical_summary       TEXT,
    urgency                VARCHAR(20)  NOT NULL DEFAULT 'ROUTINE'
                               CHECK (urgency IN ('ROUTINE','URGENT','EMERGENT')),
    status                 VARCHAR(50)  NOT NULL DEFAULT 'DRAFT'
                               CHECK (status IN ('DRAFT','SUBMITTED','ACCEPTED','COMPLETED','CANCELLED')),
    delivery_method        VARCHAR(50)
                               CHECK (delivery_method IN ('OCEAN_EREFERRAL','PDF_FAX','PDF_PRINT')),
    external_referral_id   VARCHAR(255),
    -- Referral letter PDF URL in object storage (fallback path)
    document_url           VARCHAR(500),
    submitted_at           TIMESTAMPTZ,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);


-- =============================================================
-- DIAGNOSTIC REQUISITIONS
-- FHIR: ServiceRequest
-- =============================================================

CREATE TABLE diagnostic_requisitions (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    care_plan_id          UUID         NOT NULL REFERENCES care_plans(id),
    patient_id            UUID         NOT NULL REFERENCES users(id),
    ordering_physician_id UUID         NOT NULL REFERENCES users(id),
    test_type             VARCHAR(100) NOT NULL,
    test_codes            JSONB        NOT NULL DEFAULT '[]'::jsonb,
    clinical_indication   TEXT,
    urgency               VARCHAR(20)  NOT NULL DEFAULT 'ROUTINE'
                              CHECK (urgency IN ('ROUTINE','URGENT','STAT')),
    status                VARCHAR(50)  NOT NULL DEFAULT 'DRAFT'
                              CHECK (status IN ('DRAFT','SENT','BOOKED','IN_PROGRESS','RESULTED','CANCELLED')),
    delivery_method       VARCHAR(50)
                              CHECK (delivery_method IN ('ELECTRONIC','PDF_PRINT')),
    diagnostic_centre_id  UUID         REFERENCES diagnostic_centres(id),
    -- Printable requisition PDF URL in object storage (fallback path)
    document_url          VARCHAR(500),
    appointment_at        TIMESTAMPTZ,
    sent_at               TIMESTAMPTZ,
    booked_at             TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);


-- =============================================================
-- DIAGNOSTIC RESULTS
-- FHIR: DiagnosticReport
-- =============================================================

-- released_to_patient_at is NULL until physician adds interpretation.
-- Patient visibility gated on this column at the service layer.
CREATE TABLE diagnostic_results (
    id                       UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    requisition_id           UUID        NOT NULL REFERENCES diagnostic_requisitions(id),
    patient_id               UUID        NOT NULL REFERENCES users(id),
    ordering_physician_id    UUID        NOT NULL REFERENCES users(id),
    raw_result_text          TEXT,
    raw_result_data          JSONB,
    ai_summary               TEXT,
    physician_interpretation TEXT,
    released_to_patient_at   TIMESTAMPTZ,
    received_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- =============================================================
-- NOTIFICATIONS
-- Every event produces both an EMAIL row and an IN_APP row.
-- =============================================================

CREATE TABLE notifications (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type       VARCHAR(100) NOT NULL
                   CHECK (type IN (
                       'EMERGENCY_ALERT',
                       'URGENCY_UPDATED',
                       'PHYSICIAN_ASSIGNED',
                       'FOLLOW_UP_RECEIVED',
                       'FOLLOW_UP_PROMPT',
                       'PRESCRIPTION_SENT',
                       'PRESCRIPTION_FILLED',
                       'REQUISITION_BOOKED',
                       'APPOINTMENT_REMINDER',
                       'RESULT_READY',
                       'CARE_PLAN_UPDATE',
                       'SYMPTOM_UPDATE_PROMPT',
                       'DETERIORATION_ALERT',
                       'REFERRAL_UPDATE'
                   )),
    channel    VARCHAR(20)  NOT NULL CHECK (channel IN ('EMAIL','IN_APP')),
    title      VARCHAR(255) NOT NULL,
    body       TEXT         NOT NULL,
    sent_at    TIMESTAMPTZ,
    read_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);


-- =============================================================
-- AUDIT LOGS (PHIPA — immutable, append-only)
-- Never UPDATE or DELETE rows from this table.
-- =============================================================

CREATE TABLE audit_logs (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         REFERENCES users(id),
    action        VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id   UUID,
    ip_address    INET,
    user_agent    TEXT,
    details       JSONB,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);


-- =============================================================
-- INDEXES
-- =============================================================

CREATE INDEX idx_clinic_capabilities_clinic      ON clinic_capabilities(clinic_id);

CREATE INDEX idx_clinic_staff_clinic             ON clinic_staff(clinic_id);
CREATE INDEX idx_clinic_staff_user               ON clinic_staff(user_id);

CREATE INDEX idx_patient_profiles_user           ON patient_profiles(user_id);
CREATE INDEX idx_patient_health_coverage_patient ON patient_health_coverage(patient_id);
CREATE INDEX idx_patient_clinic_reg_patient      ON patient_clinic_registrations(patient_id);
CREATE INDEX idx_patient_clinic_reg_clinic       ON patient_clinic_registrations(clinic_id);
CREATE INDEX idx_patient_clinic_reg_physician    ON patient_clinic_registrations(assigned_physician_id);
CREATE INDEX idx_patient_consents_patient        ON patient_consents(patient_id);

CREATE INDEX idx_symptom_episodes_patient        ON symptom_episodes(patient_id);
CREATE INDEX idx_symptom_episodes_physician      ON symptom_episodes(assigned_physician_id);
CREATE INDEX idx_symptom_episodes_clinic         ON symptom_episodes(assigned_clinic_id);
CREATE INDEX idx_symptom_episodes_status         ON symptom_episodes(status);
CREATE INDEX idx_symptom_episodes_ai_urgency     ON symptom_episodes(ai_urgency_level);

CREATE INDEX idx_symptom_submissions_episode     ON symptom_submissions(episode_id);
CREATE INDEX idx_symptom_submissions_patient     ON symptom_submissions(patient_id);
CREATE INDEX idx_symptom_submissions_pending     ON symptom_submissions(processing_status)
    WHERE processing_status IN ('PENDING','PROCESSING');

CREATE INDEX idx_care_plans_episode              ON care_plans(episode_id);
CREATE INDEX idx_care_plans_patient              ON care_plans(patient_id);
CREATE INDEX idx_care_plans_physician            ON care_plans(physician_id);
CREATE INDEX idx_care_plans_clinic               ON care_plans(clinic_id);

CREATE INDEX idx_follow_ups_care_plan            ON follow_ups(care_plan_id);

CREATE INDEX idx_prescriptions_care_plan         ON prescriptions(care_plan_id);
CREATE INDEX idx_prescriptions_patient           ON prescriptions(patient_id);
CREATE INDEX idx_prescriptions_status            ON prescriptions(status);

CREATE INDEX idx_referrals_care_plan             ON referrals(care_plan_id);
CREATE INDEX idx_referrals_patient               ON referrals(patient_id);

CREATE INDEX idx_diagnostic_req_care_plan        ON diagnostic_requisitions(care_plan_id);
CREATE INDEX idx_diagnostic_req_patient          ON diagnostic_requisitions(patient_id);
CREATE INDEX idx_diagnostic_req_status           ON diagnostic_requisitions(status);

CREATE INDEX idx_diagnostic_results_requisition  ON diagnostic_results(requisition_id);
CREATE INDEX idx_diagnostic_results_patient      ON diagnostic_results(patient_id);
CREATE INDEX idx_diagnostic_results_unreleased   ON diagnostic_results(ordering_physician_id)
    WHERE released_to_patient_at IS NULL;

CREATE INDEX idx_notifications_user              ON notifications(user_id);
CREATE INDEX idx_notifications_unread            ON notifications(user_id)
    WHERE read_at IS NULL;

CREATE INDEX idx_audit_logs_user                 ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_resource             ON audit_logs(resource_type, resource_id);
CREATE INDEX idx_audit_logs_created_at           ON audit_logs(created_at);
