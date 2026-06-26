package com.medsimple.wearable.entity;

/**
 * The wearable / device metrics the platform ingests, each with its display label, unit, and the
 * LOINC code it maps to as a FHIR Observation. Consumer-grade only — see {@link WearableSource}
 * for provenance, which is always carried alongside so a reading is never mistaken for a
 * clinical-grade measurement.
 */
public enum WearableMetric {
    HEART_RATE("Heart rate", "bpm", "8867-4"),
    RESTING_HEART_RATE("Resting heart rate", "bpm", "40443-4"),
    STEPS("Steps", "count", "55423-8"),
    SLEEP_MINUTES("Sleep", "min", "93832-4"),
    SPO2("Blood oxygen", "%", "59408-5");

    private final String label;
    private final String unit;
    private final String loinc;

    WearableMetric(String label, String unit, String loinc) {
        this.label = label;
        this.unit = unit;
        this.loinc = loinc;
    }

    public String label() {
        return label;
    }

    /** Canonical unit for this metric; readings are normalized to it on ingest. */
    public String unit() {
        return unit;
    }

    /** LOINC code for the FHIR Observation this metric maps to. */
    public String loinc() {
        return loinc;
    }
}
