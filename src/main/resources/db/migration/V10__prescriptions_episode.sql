-- Re-parent prescriptions to the episode (the encounter) — the last order type to move off the
-- care plan. The care plan becomes an optional link, finishing the consistency: every order
-- (diagnostics, referrals, prescriptions, appointments, suggestions) hangs off the episode.

ALTER TABLE prescriptions ADD COLUMN episode_id UUID REFERENCES symptom_episodes(id);

UPDATE prescriptions p
   SET episode_id = cp.episode_id
  FROM care_plans cp
 WHERE p.care_plan_id = cp.id;

ALTER TABLE prescriptions ALTER COLUMN episode_id SET NOT NULL;
ALTER TABLE prescriptions ALTER COLUMN care_plan_id DROP NOT NULL;

CREATE INDEX idx_prescriptions_episode ON prescriptions(episode_id);
