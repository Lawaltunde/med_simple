-- The AI's suggested follow-up interval (in days) on a care suggestion. The physician approves
-- it into an actual appointment from the console. Nullable: many suggestions have no follow-up.

ALTER TABLE care_suggestions ADD COLUMN suggested_followup_days INT;
