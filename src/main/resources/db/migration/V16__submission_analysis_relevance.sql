-- Whether a patient-submitted PHOTO actually shows a medical symptom, set by the photo analysis
-- step. Lets the platform avoid forwarding a non-medical image to the physician as a confident
-- clinical finding, and ask the patient to retake or describe it. NULL for text/voice submissions.
-- NO_SYMPTOM / UNSURE submissions are never silently dropped — they still reach the physician,
-- flagged.

ALTER TABLE symptom_submissions
    ADD COLUMN ai_analysis_relevance VARCHAR(20)
        CHECK (ai_analysis_relevance IN ('SYMPTOM_PRESENT', 'NO_SYMPTOM', 'UNSURE'));
