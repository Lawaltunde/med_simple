package com.medsimple.ai.service;

import com.medsimple.careplan.repository.CareSuggestionRepository;
import com.medsimple.careplan.repository.projection.ApprovedPlanExample;
import com.medsimple.patient.entity.ConsentType;
import com.medsimple.patient.service.PatientService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanRetrieverTest {

    @Mock CareSuggestionRepository careSuggestionRepository;
    @Mock PatientService patientService;

    @InjectMocks PlanRetriever planRetriever;

    private static ApprovedPlanExample example(UUID patientId, String planNotes, String aiSummary) {
        return new ApprovedPlanExample() {
            public UUID getPatientId() { return patientId; }
            public String getPlanNotes() { return planNotes; }
            public String getAiSummary() { return aiSummary; }
        };
    }

    @Test
    void ranksByKeywordOverlapWithTheAiSummary() {
        UUID p = UUID.randomUUID();
        ApprovedPlanExample relevant = example(p, "Throat plan: supportive care, fluids.",
                "Patient has a sore throat and fever for two days.");
        ApprovedPlanExample irrelevant = example(p, "Ankle plan: rest, ice, elevation.",
                "Patient twisted an ankle playing soccer.");
        when(careSuggestionRepository.findApprovedExemplars(anyInt()))
                .thenReturn(List.of(irrelevant, relevant));
        when(patientService.hasActiveConsent(p, ConsentType.AI_TRAINING)).thenReturn(true);

        List<String> out = planRetriever.retrieveExemplars("sore throat and fever, painful swallowing", 3);

        assertThat(out).containsExactly("Throat plan: supportive care, fluids.");
    }

    @Test
    void excludesPlansFromNonConsentedPatients() {
        UUID consented = UUID.randomUUID();
        UUID notConsented = UUID.randomUUID();
        when(careSuggestionRepository.findApprovedExemplars(anyInt())).thenReturn(List.of(
                example(notConsented, "No-consent plan for cough.", "Patient has a cough and cold."),
                example(consented, "Consented plan for cough.", "Patient has a cough and congestion.")));
        when(patientService.hasActiveConsent(notConsented, ConsentType.AI_TRAINING)).thenReturn(false);
        when(patientService.hasActiveConsent(consented, ConsentType.AI_TRAINING)).thenReturn(true);

        List<String> out = planRetriever.retrieveExemplars("cough and cold for a few days", 3);

        assertThat(out).containsExactly("Consented plan for cough.");
    }

    @Test
    void emptyWhenNoKeywordMatch() {
        UUID p = UUID.randomUUID();
        lenient().when(patientService.hasActiveConsent(p, ConsentType.AI_TRAINING)).thenReturn(true);
        when(careSuggestionRepository.findApprovedExemplars(anyInt()))
                .thenReturn(List.of(example(p, "Migraine plan.", "Patient has a throbbing headache.")));

        List<String> out = planRetriever.retrieveExemplars("sprained wrist after a fall", 3);

        assertThat(out).isEmpty();
    }

    @Test
    void emptyWhenBlankInputOrZeroK() {
        assertThat(planRetriever.retrieveExemplars("   ", 3)).isEmpty();
        assertThat(planRetriever.retrieveExemplars("sore throat", 0)).isEmpty();
    }
}
