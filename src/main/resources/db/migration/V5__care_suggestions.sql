-- AI clinical suggestion: an advisory plan of care the AI proposes at triage time.
-- It is NOT a care plan. It is a separate, auditable artifact with its own lifecycle
-- (PROPOSED -> APPROVED/EDITED/REJECTED, or FAILED when suppressed/guardrail-blocked).
-- A signed care_plans row links back to the suggestion it came from via source_suggestion_id.
-- The patient has NO read path to this table; visibility is structural (see design doc §3).

CREATE TABLE care_suggestions (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    episode_id      UUID         NOT NULL REFERENCES symptom_episodes(id),
    patient_id      UUID         NOT NULL REFERENCES users(id),

    -- AI output, advisory only. Nullable: a FAILED / GUARDRAIL_BLOCKED suggestion stores
    -- no plan text (we never persist a blocked proposal).
    suggested_plan  TEXT,
    rationale       TEXT,

    -- provenance: which adapter produced it and (for the real adapter) which model.
    adapter         VARCHAR(50)  NOT NULL,
    model           VARCHAR(100),

    -- lifecycle.
    status          VARCHAR(50)  NOT NULL DEFAULT 'PROPOSED'
                        CHECK (status IN ('PROPOSED','APPROVED','EDITED','REJECTED','FAILED')),
    reject_reason   VARCHAR(100),
    reviewed_by     UUID         REFERENCES users(id),
    reviewed_at     TIMESTAMPTZ,
    care_plan_id    UUID         REFERENCES care_plans(id),

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_care_suggestions_episode ON care_suggestions(episode_id);
CREATE INDEX idx_care_suggestions_patient ON care_suggestions(patient_id);
CREATE INDEX idx_care_suggestions_status  ON care_suggestions(status);

-- Link a signed plan back to the suggestion it came from. Nullable: plans can still be
-- written from scratch with no AI involvement, preserving care_plans.physician_id NOT NULL.
ALTER TABLE care_plans ADD COLUMN source_suggestion_id UUID REFERENCES care_suggestions(id);
