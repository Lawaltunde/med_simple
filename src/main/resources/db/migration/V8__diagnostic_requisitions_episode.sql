-- Re-parent diagnostic requisitions to the episode (the encounter) so a physician can order a
-- work-up WITHOUT first writing a care plan. The care plan becomes an optional link, consistent
-- with how appointments and care_suggestions already hang off the episode.

ALTER TABLE diagnostic_requisitions ADD COLUMN episode_id UUID REFERENCES symptom_episodes(id);

-- Backfill from the care plan each existing requisition already points at.
UPDATE diagnostic_requisitions r
   SET episode_id = cp.episode_id
  FROM care_plans cp
 WHERE r.care_plan_id = cp.id;

ALTER TABLE diagnostic_requisitions ALTER COLUMN episode_id SET NOT NULL;
ALTER TABLE diagnostic_requisitions ALTER COLUMN care_plan_id DROP NOT NULL;

CREATE INDEX idx_diagnostic_requisitions_episode ON diagnostic_requisitions(episode_id);
