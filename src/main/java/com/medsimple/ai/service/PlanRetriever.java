package com.medsimple.ai.service;

import com.medsimple.careplan.repository.CareSuggestionRepository;
import com.medsimple.careplan.repository.projection.ApprovedPlanExample;
import com.medsimple.patient.entity.ConsentType;
import com.medsimple.patient.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Keyword-first RAG. Retrieves physician-approved plans from the corpus that match the current
 * presentation, to feed the model as few-shot exemplars ("what physicians here approved for
 * similar cases"). Vanilla by design: token overlap, no embeddings / vector store. If the evals
 * show this is not enough, embeddings + pgvector earn their complexity then.
 *
 * PHI: only the approved PLAN text (clinician-authored care instructions) is returned for
 * injection, and only from patients with an active AI_TRAINING consent. The matching signal (the
 * stored AI summary) never leaves this service.
 */
@Service
@RequiredArgsConstructor
public class PlanRetriever {

    private static final int CORPUS_LIMIT = 200;

    private final CareSuggestionRepository careSuggestionRepository;
    private final PatientService patientService;

    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "for", "with", "that", "this", "have", "has", "not", "you", "your",
            "are", "was", "were", "but", "from", "they", "their", "them", "had", "what", "when",
            "will", "would", "should", "could", "been", "being", "into", "over", "under", "more",
            "most", "some", "any", "all", "can", "may", "might", "also", "than", "then", "there",
            "here", "about", "after", "before", "since", "past", "days", "week", "weeks", "ago",
            "still", "feel", "feeling", "patient", "reports");

    private record Scored(UUID patientId, String plan, int score) {}

    // Top-k approved plan texts whose AI summary best overlaps the current symptoms, from
    // consented patients only. Empty when there is no corpus, no keyword match, or no consent.
    @Transactional(readOnly = true)
    public List<String> retrieveExemplars(String currentSymptomText, int k) {
        if (currentSymptomText == null || currentSymptomText.isBlank() || k <= 0) {
            return List.of();
        }
        Set<String> query = tokens(currentSymptomText);
        if (query.isEmpty()) {
            return List.of();
        }

        return careSuggestionRepository.findApprovedExemplars(CORPUS_LIMIT).stream()
                .filter(e -> e.getPlanNotes() != null && !e.getPlanNotes().isBlank())
                .map(e -> new Scored(e.getPatientId(), e.getPlanNotes().trim(),
                        overlap(query, tokens(e.getAiSummary()))))
                .filter(s -> s.score() > 0)
                .sorted(Comparator.comparingInt(Scored::score).reversed())
                // Consent-gate only the keyword matches, in score order, until we have k.
                .filter(s -> patientService.hasActiveConsent(s.patientId(), ConsentType.AI_TRAINING))
                .map(Scored::plan)
                .distinct()
                .limit(k)
                .toList();
    }

    private static int overlap(Set<String> a, Set<String> b) {
        int n = 0;
        for (String t : a) {
            if (b.contains(t)) n++;
        }
        return n;
    }

    private static Set<String> tokens(String text) {
        if (text == null) return Set.of();
        return Arrays.stream(text.toLowerCase().split("[^a-z]+"))
                .filter(t -> t.length() >= 4 && !STOPWORDS.contains(t))
                .collect(Collectors.toSet());
    }
}
