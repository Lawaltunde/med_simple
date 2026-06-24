-- The AI's suggested ORDERS on a care suggestion: a referral specialty and diagnostic tests. The
-- physician one-click-prefills the referral / diagnostic create form from these, then reviews and
-- submits through the existing flow — the AI proposes, the physician decides.
--
-- Both are advisory and staff-only (the patient never reads care_suggestions). They are populated
-- only after the deterministic OrderSuggestionGuardrail has dropped any invasive/controlled test and
-- sanitized the specialty to a recognised value. Nullable: most suggestions have neither.
--
-- suggested_tests stores the sanitized tests as a simple newline-delimited string (kept simple per
-- the spec — no JSON parsing needed; the entity splits/joins on '\n').

ALTER TABLE care_suggestions ADD COLUMN suggested_referral_specialty VARCHAR(100);
ALTER TABLE care_suggestions ADD COLUMN suggested_tests TEXT;
