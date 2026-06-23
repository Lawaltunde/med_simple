-- FHIR: Appointment. A scheduled visit/follow-up between a patient and a physician at a clinic,
-- optionally tied to the episode and care plan it follows from. Surfaced as a calendar card in
-- both the patient app and the clinician console, and exportable as an .ics "add to calendar".

CREATE TABLE appointments (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id       UUID         NOT NULL REFERENCES users(id),
    physician_id     UUID         NOT NULL REFERENCES users(id),
    clinic_id        UUID         NOT NULL REFERENCES clinics(id),
    episode_id       UUID         REFERENCES symptom_episodes(id),
    care_plan_id     UUID         REFERENCES care_plans(id),

    scheduled_at     TIMESTAMPTZ  NOT NULL,
    duration_minutes INT          NOT NULL DEFAULT 15,
    reason           VARCHAR(500),
    status           VARCHAR(30)  NOT NULL DEFAULT 'SCHEDULED'
                         CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),

    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_appointments_patient   ON appointments(patient_id);
CREATE INDEX idx_appointments_clinic    ON appointments(clinic_id);
CREATE INDEX idx_appointments_physician ON appointments(physician_id);
CREATE INDEX idx_appointments_scheduled ON appointments(scheduled_at);
