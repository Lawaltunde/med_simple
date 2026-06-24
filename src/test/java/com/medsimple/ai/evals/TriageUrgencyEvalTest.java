package com.medsimple.ai.evals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medsimple.ai.dao.AiTriageDao;
import com.medsimple.ai.dto.EpisodeContext;
import com.medsimple.ai.dto.TriageResult;
import com.medsimple.symptom.entity.UrgencyLevel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Paid quality eval (Lane 2, CLAUDE.md) for the OpenAI triage adapter's URGENCY classification.
 * Runs the REAL adapter against a labelled set. Excluded from the gate suite (@Tag("eval")); run:
 *   ./mvnw test -Pevals -Dspring-boot.run.profiles=local
 * Needs the local OpenAI key (application-local.properties) and the dev DB up.
 *
 * Safety contract this measures:
 *   1) NO missed emergencies. Every case labelled `emergency` must be classified EMERGENCY.
 *      This is the patient-safety floor — threshold is 100%, a single miss fails the suite.
 *   2) Overall urgency accuracy (the model's level is in the case's acceptable set) >= threshold.
 *      Acceptable sets are deliberately a band (e.g. SEE_SOON or URGENT) because reasonable
 *      clinicians disagree at the margins; under-triaging an emergency is never acceptable.
 */
@Tag("eval")
@SpringBootTest(properties = "ai.triage.adapter=openai")
@ActiveProfiles("local")
class TriageUrgencyEvalTest {

    private static final double ACCURACY_THRESHOLD = 0.80;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired AiTriageDao aiTriageDao;   // OpenAI adapter (@Primary when ai.triage.adapter=openai)

    record EvalCase(String id, String category, String symptomText, List<String> acceptableUrgency) {}

    @Test
    void urgencyClassificationMeetsSafetyAndAccuracyThresholds() throws Exception {
        List<EvalCase> cases = List.of(
                MAPPER.readValue(new File("evals/triage-urgency/cases.json"), EvalCase[].class));

        List<String> missedEmergencies = new ArrayList<>();
        int correct = 0;
        StringBuilder report = new StringBuilder("\n=== Triage-urgency eval ===\n");

        for (EvalCase c : cases) {
            TriageResult result = aiTriageDao.analyze(
                    new EpisodeContext(UUID.randomUUID(), List.of(c.symptomText())));
            UrgencyLevel got = result.urgencyLevel();
            boolean inSet = c.acceptableUrgency().contains(got.name());
            if (inSet) {
                correct++;
            }
            if ("emergency".equals(c.category()) && got != UrgencyLevel.EMERGENCY) {
                missedEmergencies.add(c.id() + " classified " + got);
            }
            report.append(String.format("- %-34s got=%-9s acceptable=%-22s %s%n",
                    c.id(), got, c.acceptableUrgency(), inSet ? "ok" : "MISS"));
        }

        double accuracy = (double) correct / cases.size();
        report.append(String.format(
                "missedEmergencies=%d  accuracy=%.2f (%d/%d, threshold=%.2f)%n",
                missedEmergencies.size(), accuracy, correct, cases.size(), ACCURACY_THRESHOLD));
        System.out.println(report);

        assertThat(missedEmergencies)
                .as("every emergency case must be classified EMERGENCY (no under-triage)").isEmpty();
        assertThat(accuracy).as("urgency accuracy (got in acceptable set)")
                .isGreaterThanOrEqualTo(ACCURACY_THRESHOLD);
    }
}
