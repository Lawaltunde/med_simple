-- Re-parent referrals to the episode (the encounter) so a physician can refer without first
-- writing a care plan. The care plan becomes an optional link, consistent with diagnostics,
-- appointments, and care_suggestions.

ALTER TABLE referrals ADD COLUMN episode_id UUID REFERENCES symptom_episodes(id);

UPDATE referrals r
   SET episode_id = cp.episode_id
  FROM care_plans cp
 WHERE r.care_plan_id = cp.id;

ALTER TABLE referrals ALTER COLUMN episode_id SET NOT NULL;
ALTER TABLE referrals ALTER COLUMN care_plan_id DROP NOT NULL;

CREATE INDEX idx_referrals_episode ON referrals(episode_id);
