package com.medsimple.careplan.repository.projection;

import java.util.UUID;

// One row of the RAG corpus: a physician-approved plan, the AI summary it was for (the matching
// signal, kept server-side), and the source patient (whose AI_TRAINING consent is checked before
// the plan is ever used as an exemplar).
public interface ApprovedPlanExample {
    UUID getPatientId();
    String getPlanNotes();
    String getAiSummary();
}
