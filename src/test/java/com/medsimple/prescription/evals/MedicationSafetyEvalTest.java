package com.medsimple.prescription.evals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medsimple.prescription.dao.MedicationContext;
import com.medsimple.prescription.dao.MedicationSafetyDao;
import com.medsimple.prescription.dao.SafetyCheckResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Paid quality eval (Lane 2, CLAUDE.md) for the OpenAI medication-safety adapter.
 * Runs the REAL adapter against a labelled set. Excluded from the gate suite (@Tag("eval")); run:
 *   ./mvnw test -Pevals -Dspring-boot.run.profiles=local
 * Needs the local OpenAI key (application-local.properties) and the dev DB up.
 *
 * Safety contract this measures (the dangerous failure is a false "all clear"):
 *   1) Every known allergy case must raise an allergy flag naming the offending class.
 *      Deterministic check on allergyFlags text. Threshold: 100%.
 *   2) Every known interaction case must raise an interaction naming the offending current med.
 *      Deterministic check on interactions text. Threshold: 100%.
 *   3) Clean cases must NOT be flagged for an allergy or interaction (no false alarms that train
 *      clinicians to ignore the tool). Tracked and reported; threshold: zero false positives.
 * mustFlag / mustNotMissInteraction tokens are matched case-insensitively against the model's
 * own finding text, so this catches a model that "checks" but words the finding around the drug.
 */
@Tag("eval")
@SpringBootTest(properties = "ai.safety.adapter=openai")
@ActiveProfiles("local")
class MedicationSafetyEvalTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired MedicationSafetyDao medicationSafetyDao;   // OpenAI adapter when ai.safety.adapter=openai

    record EvalCase(String id, String category, String medication, String dosage, String frequency,
                    List<String> currentMeds, List<String> allergies,
                    String mustFlag, String mustNotMissInteraction,
                    boolean expectAllergyFlag, boolean expectInteraction, String goldThemes) {}

    @Test
    void medicationSafetyFlagsEveryKnownRisk() throws Exception {
        List<EvalCase> cases = List.of(
                MAPPER.readValue(new File("evals/medication-safety/cases.json"), EvalCase[].class));

        List<String> missedAllergies = new ArrayList<>();
        List<String> missedInteractions = new ArrayList<>();
        List<String> falsePositives = new ArrayList<>();
        StringBuilder report = new StringBuilder("\n=== Medication-safety eval ===\n");

        for (EvalCase c : cases) {
            SafetyCheckResult r = medicationSafetyDao.check(new MedicationContext(
                    c.medication(), c.dosage(), c.frequency(),
                    c.currentMeds() == null ? List.of() : c.currentMeds(),
                    c.allergies() == null ? List.of() : c.allergies()));

            String allergyText = String.join(" | ", r.allergyFlags()).toLowerCase();
            String interactionText = String.join(" | ", r.interactions()).toLowerCase();

            boolean allergyRaised = !r.allergyFlags().isEmpty();
            boolean interactionRaised = !r.interactions().isEmpty();

            if (c.expectAllergyFlag()) {
                String token = c.mustFlag() == null ? "" : c.mustFlag().toLowerCase();
                if (!allergyRaised || !allergyText.contains(token)) {
                    missedAllergies.add(c.id() + " (wanted allergy flag mentioning '" + c.mustFlag()
                            + "', got " + r.allergyFlags() + ")");
                }
            }
            if (c.expectInteraction()) {
                String token = c.mustNotMissInteraction() == null ? "" : c.mustNotMissInteraction().toLowerCase();
                if (!interactionRaised || !interactionText.contains(token)) {
                    missedInteractions.add(c.id() + " (wanted interaction mentioning '"
                            + c.mustNotMissInteraction() + "', got " + r.interactions() + ")");
                }
            }
            if ("clean".equals(c.category()) && (allergyRaised || interactionRaised)) {
                falsePositives.add(c.id() + " allergyFlags=" + r.allergyFlags()
                        + " interactions=" + r.interactions());
            }

            report.append(String.format("- %-34s allergy=%-3s interaction=%-3s doseWarn=%d%n",
                    c.id(), allergyRaised ? "yes" : "no", interactionRaised ? "yes" : "no",
                    r.doseWarnings().size()));
        }

        report.append(String.format(
                "missedAllergies=%d  missedInteractions=%d  falsePositives=%d%n",
                missedAllergies.size(), missedInteractions.size(), falsePositives.size()));
        if (!missedAllergies.isEmpty()) report.append("  missedAllergies: ").append(missedAllergies).append("\n");
        if (!missedInteractions.isEmpty()) report.append("  missedInteractions: ").append(missedInteractions).append("\n");
        if (!falsePositives.isEmpty()) report.append("  falsePositives: ").append(falsePositives).append("\n");
        System.out.println(report);

        assertThat(missedAllergies)
                .as("every known allergy must be flagged (no false 'all clear')").isEmpty();
        assertThat(missedInteractions)
                .as("every known interaction must be flagged (no false 'all clear')").isEmpty();
        assertThat(falsePositives)
                .as("clean cases must not raise spurious allergy/interaction flags").isEmpty();
    }
}
