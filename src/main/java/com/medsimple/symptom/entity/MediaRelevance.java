package com.medsimple.symptom.entity;

/**
 * Whether a patient-submitted image actually shows a medical symptom. Set by the photo analysis
 * step so a non-medical image is never forwarded to the physician as a confident clinical finding,
 * and so the patient can be asked to retake or describe it. Never used to silently discard a
 * submission — UNSURE and NO_SYMPTOM still reach the physician, clearly flagged.
 */
public enum MediaRelevance {
    /** A visible medical symptom is present (rash, wound, swelling, discoloration, ...). */
    SYMPTOM_PRESENT,
    /** No visible medical symptom — the image does not appear to be of a clinical concern. */
    NO_SYMPTOM,
    /** The image could not be confidently classified (blurry, ambiguous, or analysis failed). */
    UNSURE
}
