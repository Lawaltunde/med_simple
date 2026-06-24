package com.medsimple.referral.evals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medsimple.referral.dao.SpecialtySuggestion;
import com.medsimple.referral.dao.SpecialtySuggestionDao;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Paid quality eval (Lane 2, CLAUDE.md) for the OpenAI specialty-suggestion adapter.
 * Runs the REAL adapter against a labelled set. Excluded from the gate suite (@Tag("eval")); run:
 *   ./mvnw test -Pevals -Dspring-boot.run.profiles=local
 * Needs the local OpenAI key (application-local.properties) and the dev DB up.
 *
 * What it measures: for each clinical summary the suggested specialty must match the case's
 * acceptable set (synonyms allowed, e.g. ENT / Otolaryngology). Match is case-insensitive and
 * substring-tolerant so "Orthopedic Surgery" satisfies an "Orthopedics" gold. This is the right
 * specialty routing — a wrong referral wastes a specialist slot and delays care.
 * Threshold: accuracy >= 0.85.
 */
@Tag("eval")
@SpringBootTest(properties = "ai.referral.adapter=openai")
@ActiveProfiles("local")
class SpecialtySuggestionEvalTest {

    private static final double ACCURACY_THRESHOLD = 0.85;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired SpecialtySuggestionDao specialtySuggestionDao;   // OpenAI adapter when ai.referral.adapter=openai

    record EvalCase(String id, String category, String clinicalSummary, List<String> acceptableSpecialty) {}

    @Test
    void specialtySuggestionMeetsAccuracyThreshold() throws Exception {
        List<EvalCase> cases = List.of(
                MAPPER.readValue(new File("evals/specialty-suggestion/cases.json"), EvalCase[].class));

        int correct = 0;
        List<String> misses = new ArrayList<>();
        StringBuilder report = new StringBuilder("\n=== Specialty-suggestion eval ===\n");

        for (EvalCase c : cases) {
            SpecialtySuggestion s = specialtySuggestionDao.suggest(c.clinicalSummary());
            String got = s.specialty() == null ? "" : s.specialty().toLowerCase(Locale.ROOT).trim();
            boolean match = !got.isEmpty() && c.acceptableSpecialty().stream()
                    .map(a -> a.toLowerCase(Locale.ROOT).trim())
                    .anyMatch(a -> got.contains(a) || a.contains(got));
            if (match) {
                correct++;
            } else {
                misses.add(c.id() + " got='" + s.specialty() + "' wanted " + c.acceptableSpecialty());
            }
            report.append(String.format("- %-26s got=%-22s acceptable=%-30s %s%n",
                    c.id(), s.specialty(), c.acceptableSpecialty(), match ? "ok" : "MISS"));
        }

        double accuracy = (double) correct / cases.size();
        report.append(String.format("accuracy=%.2f (%d/%d, threshold=%.2f)%n",
                accuracy, correct, cases.size(), ACCURACY_THRESHOLD));
        if (!misses.isEmpty()) report.append("  misses: ").append(misses).append("\n");
        System.out.println(report);

        assertThat(accuracy).as("specialty suggestion accuracy (match to acceptable set)")
                .isGreaterThanOrEqualTo(ACCURACY_THRESHOLD);
    }
}
