package com.medsimple.ai.service;

import com.medsimple.ai.dao.AiTriageDao;
import com.medsimple.ai.dto.SuggestedCare;
import com.medsimple.ai.dto.TriageResult;
import com.medsimple.audit.service.AuditService;
import com.medsimple.careplan.entity.CareSuggestion;
import com.medsimple.careplan.entity.CareSuggestionStatus;
import com.medsimple.careplan.repository.CareSuggestionRepository;
import com.medsimple.careplan.service.OrderSuggestionGuardrail;
import com.medsimple.careplan.service.SuggestionGuardrail;
import com.medsimple.shared.privacy.SymptomRedactor;
import com.medsimple.shared.entity.User;
import com.medsimple.shared.entity.UserRole;
import com.medsimple.symptom.dto.EpisodeResponse;
import com.medsimple.symptom.entity.*;
import com.medsimple.symptom.exception.EpisodeNotFoundException;
import com.medsimple.symptom.repository.SymptomEpisodeRepository;
import com.medsimple.symptom.repository.SymptomSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiTriageServiceTest {

    @Mock AiTriageDao aiTriageDao;
    @Mock SymptomEpisodeRepository episodeRepository;
    @Mock SymptomSubmissionRepository submissionRepository;
    @Mock AuditService auditService;
    @Mock com.medsimple.notification.service.NotificationService notificationService;
    @Mock com.medsimple.auth.security.AccessGuard accessGuard;
    @Mock CareSuggestionRepository careSuggestionRepository;
    @Mock SuggestionGuardrail suggestionGuardrail;
    @Mock OrderSuggestionGuardrail orderSuggestionGuardrail;
    @Mock PlanRetriever planRetriever;
    // Real redactor (not a mock): the wiring test below proves PHI is actually stripped before the
    // adapter sees it, and clinical fixtures ("headache") pass through unchanged.
    @Spy SymptomRedactor symptomRedactor = new SymptomRedactor();

    @InjectMocks AiTriageService aiTriageService;

    private User patient;
    private SymptomEpisode episode;
    private UUID episodeId;

    @BeforeEach
    void setUp() {
        episodeId = UUID.randomUUID();
        patient = User.builder()
                .id(UUID.randomUUID())
                .email("jane@example.com")
                .role(UserRole.PATIENT)
                .firstName("Jane").lastName("Doe")
                .createdAt(OffsetDateTime.now()).build();

        episode = SymptomEpisode.builder()
                .id(episodeId)
                .patient(patient)
                .status(EpisodeStatus.SUBMITTED)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        // Most non-emergency triages persist a care suggestion; give save an id for the audit log.
        lenient().when(careSuggestionRepository.save(any())).thenAnswer(inv -> {
            CareSuggestion c = inv.getArgument(0);
            if (c.getId() == null) c.setId(UUID.randomUUID());
            return c;
        });
        // RAG retrieval is exercised in PlanRetrieverTest; default to no exemplars here.
        lenient().when(planRetriever.retrieveExemplars(anyString(), anyInt())).thenReturn(List.of());
    }

    private SymptomSubmission textSubmission(String text) {
        return SymptomSubmission.builder()
                .id(UUID.randomUUID()).episode(episode).patient(patient)
                .inputType(InputType.TEXT).textContent(text)
                .processingStatus(ProcessingStatus.COMPLETED)
                .submittedAt(OffsetDateTime.now()).createdAt(OffsetDateTime.now()).build();
    }

    @Test
    void triage_deniedForOtherPatient_neverCallsModel() {
        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
        doThrow(new org.springframework.security.access.AccessDeniedException("denied"))
                .when(accessGuard).requirePatientSelfOrStaff(patient.getId());

        assertThatThrownBy(() -> aiTriageService.triageEpisode(episodeId))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verifyNoInteractions(aiTriageDao);
    }

    @Test
    void triage_urgentResult_setsAiAnalyzedStatus() {
        TriageResult result = new TriageResult(UrgencyLevel.URGENT, "Potential infection.");

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
        when(submissionRepository.findByEpisodeIdOrderBySubmittedAtAsc(episodeId))
                .thenReturn(List.of(textSubmission("high fever and severe abdominal pain")));
        when(aiTriageDao.analyze(any())).thenReturn(result);
        when(episodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EpisodeResponse response = aiTriageService.triageEpisode(episodeId);

        assertThat(response.status()).isEqualTo(EpisodeStatus.AI_ANALYZED);
        assertThat(response.aiUrgencyLevel()).isEqualTo(UrgencyLevel.URGENT);
        assertThat(response.aiSummary()).isEqualTo("Potential infection.");
        assertThat(response.aiAnalyzedAt()).isNotNull();
    }

    @Test
    void triage_emergency_setsEmergencyDispatchedStatus() {
        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
        when(submissionRepository.findByEpisodeIdOrderBySubmittedAtAsc(episodeId))
                .thenReturn(List.of(textSubmission("chest pain and can't breathe")));
        when(aiTriageDao.analyze(any())).thenReturn(
                new TriageResult(UrgencyLevel.EMERGENCY, "Possible cardiac event. Call 911."));
        when(episodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EpisodeResponse response = aiTriageService.triageEpisode(episodeId);

        assertThat(response.status()).isEqualTo(EpisodeStatus.EMERGENCY_DISPATCHED);
        assertThat(response.aiUrgencyLevel()).isEqualTo(UrgencyLevel.EMERGENCY);
    }

    @Test
    void triage_onlyCompletedSubmissionsSentToAi() {
        SymptomSubmission completed = textSubmission("headache");
        SymptomSubmission pending = SymptomSubmission.builder()
                .id(UUID.randomUUID()).episode(episode).patient(patient)
                .inputType(InputType.VOICE).mediaUrl("https://storage.example.com/v.mp3")
                .processingStatus(ProcessingStatus.PENDING)
                .submittedAt(OffsetDateTime.now()).createdAt(OffsetDateTime.now()).build();

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
        when(submissionRepository.findByEpisodeIdOrderBySubmittedAtAsc(episodeId))
                .thenReturn(List.of(completed, pending));
        when(aiTriageDao.analyze(argThat(ctx -> ctx.symptoms().size() == 1))).thenReturn(
                new TriageResult(UrgencyLevel.SEE_SOON, "Mild headache."));
        when(episodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        aiTriageService.triageEpisode(episodeId);

        // Verify AI only received 1 symptom (the completed one, not the pending voice)
        verify(aiTriageDao).analyze(argThat(ctx -> ctx.symptoms().size() == 1
                && ctx.symptoms().get(0).equals("headache")));
    }

    @Test
    void triage_redactsPhiFromSymptomTextBeforeReachingAdapter() {
        // A patient pastes their name, OHIP number, phone and email into the symptom box.
        SymptomSubmission leaky = textSubmission(
                "My name is John Smith, OHIP 1234-567-890, call 416-555-0199 "
                        + "or email john@example.com. I have chest pain.");

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
        when(submissionRepository.findByEpisodeIdOrderBySubmittedAtAsc(episodeId))
                .thenReturn(List.of(leaky));
        when(aiTriageDao.analyze(any())).thenReturn(
                new TriageResult(UrgencyLevel.URGENT, "Chest pain, assess promptly."));
        when(episodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        aiTriageService.triageEpisode(episodeId);

        ArgumentCaptor<com.medsimple.ai.dto.EpisodeContext> ctx =
                ArgumentCaptor.forClass(com.medsimple.ai.dto.EpisodeContext.class);
        verify(aiTriageDao).analyze(ctx.capture());

        String sent = String.join(" ", ctx.getValue().symptoms());
        // No raw PHI leaves for the model.
        assertThat(sent)
                .doesNotContain("John Smith")
                .doesNotContain("1234-567-890")
                .doesNotContain("416-555-0199")
                .doesNotContain("john@example.com");
        // The placeholders are present and the clinical signal survives.
        assertThat(sent).contains("[NAME]", "[HEALTH_CARD]", "[PHONE]", "[EMAIL]", "chest pain");
    }

    @Test
    void triage_noCompletedSubmissions_throws() {
        SymptomSubmission pending = SymptomSubmission.builder()
                .id(UUID.randomUUID()).episode(episode).patient(patient)
                .inputType(InputType.VOICE).processingStatus(ProcessingStatus.PENDING)
                .submittedAt(OffsetDateTime.now()).createdAt(OffsetDateTime.now()).build();

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
        when(submissionRepository.findByEpisodeIdOrderBySubmittedAtAsc(episodeId))
                .thenReturn(List.of(pending));

        assertThatThrownBy(() -> aiTriageService.triageEpisode(episodeId))
                .isInstanceOf(com.medsimple.shared.exception.BusinessRuleException.class)
                .hasMessageContaining("No completed submissions");
    }

    @Test
    void triage_episodeNotFound_throws() {
        when(episodeRepository.findById(episodeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiTriageService.triageEpisode(episodeId))
                .isInstanceOf(EpisodeNotFoundException.class);
    }

    @Test
    void triage_voiceTranscriptionPreferredOverTextContent() {
        SymptomSubmission voice = SymptomSubmission.builder()
                .id(UUID.randomUUID()).episode(episode).patient(patient)
                .inputType(InputType.VOICE).transcription("Patient says chest hurts badly")
                .processingStatus(ProcessingStatus.COMPLETED)
                .submittedAt(OffsetDateTime.now()).createdAt(OffsetDateTime.now()).build();

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
        when(submissionRepository.findByEpisodeIdOrderBySubmittedAtAsc(episodeId)).thenReturn(List.of(voice));
        when(aiTriageDao.analyze(argThat(ctx -> ctx.symptoms().get(0).equals("Patient says chest hurts badly"))))
                .thenReturn(new TriageResult(UrgencyLevel.URGENT, "Chest pain."));
        when(episodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        aiTriageService.triageEpisode(episodeId);

        verify(aiTriageDao).analyze(argThat(ctx ->
                ctx.symptoms().get(0).equals("Patient says chest hurts badly")));
    }

    @Test
    void triage_photoAnalysisUsedAsSymptomText() {
        // A photo-only episode: the vision adapter's description must drive triage, or the
        // episode would have no symptom text and fail. Regression for the collectSymptomTexts fix.
        SymptomSubmission photo = SymptomSubmission.builder()
                .id(UUID.randomUUID()).episode(episode).patient(patient)
                .inputType(InputType.PHOTO).mediaUrl("/api/v1/media/abc")
                .aiAnalysisResult("Erythematous rash on the left forearm")
                .processingStatus(ProcessingStatus.COMPLETED)
                .submittedAt(OffsetDateTime.now()).createdAt(OffsetDateTime.now()).build();

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
        when(submissionRepository.findByEpisodeIdOrderBySubmittedAtAsc(episodeId)).thenReturn(List.of(photo));
        when(aiTriageDao.analyze(argThat(ctx -> ctx.symptoms().get(0).equals("Erythematous rash on the left forearm"))))
                .thenReturn(new TriageResult(UrgencyLevel.SEE_SOON, "Localized rash."));
        when(episodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        aiTriageService.triageEpisode(episodeId);

        verify(aiTriageDao).analyze(argThat(ctx ->
                ctx.symptoms().size() == 1
                        && ctx.symptoms().get(0).equals("Erythematous rash on the left forearm")));
    }

    // ── AI care suggestion (gate tests) ────────────────────────────────────────

    private void stubTriage(TriageResult result) {
        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
        when(submissionRepository.findByEpisodeIdOrderBySubmittedAtAsc(episodeId))
                .thenReturn(List.of(textSubmission("symptom text")));
        when(aiTriageDao.analyze(any())).thenReturn(result);
        when(episodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private CareSuggestion captureSavedSuggestion() {
        ArgumentCaptor<CareSuggestion> captor = ArgumentCaptor.forClass(CareSuggestion.class);
        verify(careSuggestionRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void suggestion_cleanPlan_persistedAsProposedWithText() {
        stubTriage(new TriageResult(UrgencyLevel.SEE_SOON, "Viral pharyngitis.",
                new SuggestedCare("Supportive care: rest, fluids, analgesia. Safety-net advice.", "Likely viral")));
        when(suggestionGuardrail.isBlocked(anyString())).thenReturn(false);

        aiTriageService.triageEpisode(episodeId);

        CareSuggestion saved = captureSavedSuggestion();
        assertThat(saved.getStatus()).isEqualTo(CareSuggestionStatus.PROPOSED);
        assertThat(saved.getSuggestedPlan()).contains("Supportive care");
        verify(auditService).log(eq(patient.getId()), eq("CARE_SUGGESTION_GENERATED"), eq("care_suggestions"), any(), any());
    }

    @Test
    void suggestion_guardrailBlocked_persistedFailed_withNoPlanText() {
        stubTriage(new TriageResult(UrgencyLevel.SEE_SOON, "Pain.",
                new SuggestedCare("Prescribe oxycodone 10mg every 6 hours.", "pain")));
        when(suggestionGuardrail.isBlocked(anyString())).thenReturn(true);

        aiTriageService.triageEpisode(episodeId);

        CareSuggestion saved = captureSavedSuggestion();
        assertThat(saved.getStatus()).isEqualTo(CareSuggestionStatus.FAILED);
        assertThat(saved.getRejectReason()).isEqualTo(SuggestionGuardrail.BLOCK_REASON);
        // The blocked plan text must never be stored.
        assertThat(saved.getSuggestedPlan()).isNull();
    }

    @Test
    void suggestion_emergency_notPersistedAtAll() {
        stubTriage(new TriageResult(UrgencyLevel.EMERGENCY, "Call 911.",
                new SuggestedCare("should be suppressed", "x")));

        aiTriageService.triageEpisode(episodeId);

        verify(careSuggestionRepository, never()).save(any());
    }

    @Test
    void suggestion_noPlanFromAdapter_persistedFailedNoPlan() {
        // Fallback adapter / AI failure: urgency + summary only, no plan ever fabricated.
        stubTriage(new TriageResult(UrgencyLevel.ROUTINE, "Routine."));

        aiTriageService.triageEpisode(episodeId);

        CareSuggestion saved = captureSavedSuggestion();
        assertThat(saved.getStatus()).isEqualTo(CareSuggestionStatus.FAILED);
        assertThat(saved.getRejectReason()).isEqualTo("NO_PLAN");
        assertThat(saved.getSuggestedPlan()).isNull();
        verifyNoInteractions(suggestionGuardrail);
    }

    @Test
    void suggestion_persistsFollowUpDays() {
        stubTriage(new TriageResult(UrgencyLevel.SEE_SOON, "Viral URI.",
                new SuggestedCare("Supportive care.", "viral", 3)));
        when(suggestionGuardrail.isBlocked(anyString())).thenReturn(false);

        aiTriageService.triageEpisode(episodeId);

        assertThat(captureSavedSuggestion().getSuggestedFollowUpDays()).isEqualTo(3);
    }

    @Test
    void suggestion_dropsOutOfRangeFollowUp() {
        stubTriage(new TriageResult(UrgencyLevel.SEE_SOON, "Viral URI.",
                new SuggestedCare("Supportive care.", "viral", 999)));
        when(suggestionGuardrail.isBlocked(anyString())).thenReturn(false);

        aiTriageService.triageEpisode(episodeId);

        assertThat(captureSavedSuggestion().getSuggestedFollowUpDays()).isNull();
    }

    @Test
    void suggestion_persistsSanitizedReferralSpecialtyAndTests() {
        stubTriage(new TriageResult(UrgencyLevel.SEE_SOON, "Possible hypothyroidism.",
                new SuggestedCare("Check thyroid function. Refer if abnormal.", "fatigue", 14,
                        "Endocrinology", List.of("TSH", "CBC", "Liver biopsy"))));
        when(suggestionGuardrail.isBlocked(anyString())).thenReturn(false);
        // The order guardrail sanitizes: biopsy dropped, specialty canonicalised.
        when(orderSuggestionGuardrail.sanitizeTests(List.of("TSH", "CBC", "Liver biopsy")))
                .thenReturn(List.of("TSH", "CBC"));
        when(orderSuggestionGuardrail.sanitizeSpecialty("Endocrinology")).thenReturn("Endocrinology");

        aiTriageService.triageEpisode(episodeId);

        CareSuggestion saved = captureSavedSuggestion();
        assertThat(saved.getStatus()).isEqualTo(CareSuggestionStatus.PROPOSED);
        assertThat(saved.getSuggestedReferralSpecialty()).isEqualTo("Endocrinology");
        // Stored newline-delimited, biopsy never persisted.
        assertThat(saved.getSuggestedTests()).isEqualTo("TSH\nCBC");
        assertThat(saved.getSuggestedTests()).doesNotContain("biopsy");
    }

    @Test
    void suggestion_unknownSpecialtyAndAllBannedTests_storedNull() {
        stubTriage(new TriageResult(UrgencyLevel.SEE_SOON, "Plan.",
                new SuggestedCare("Supportive care.", "x", null,
                        "Witch doctor", List.of("Colonoscopy", "Biopsy"))));
        when(suggestionGuardrail.isBlocked(anyString())).thenReturn(false);
        when(orderSuggestionGuardrail.sanitizeTests(List.of("Colonoscopy", "Biopsy"))).thenReturn(null);
        when(orderSuggestionGuardrail.sanitizeSpecialty("Witch doctor")).thenReturn(null);

        aiTriageService.triageEpisode(episodeId);

        CareSuggestion saved = captureSavedSuggestion();
        assertThat(saved.getStatus()).isEqualTo(CareSuggestionStatus.PROPOSED);
        assertThat(saved.getSuggestedReferralSpecialty()).isNull();
        assertThat(saved.getSuggestedTests()).isNull();
    }

    @Test
    void suggestion_guardrailBlockedPlan_neverPersistsOrders() {
        stubTriage(new TriageResult(UrgencyLevel.SEE_SOON, "Pain.",
                new SuggestedCare("Prescribe oxycodone 10mg.", "pain", 7,
                        "Cardiology", List.of("CBC"))));
        when(suggestionGuardrail.isBlocked(anyString())).thenReturn(true);

        aiTriageService.triageEpisode(episodeId);

        CareSuggestion saved = captureSavedSuggestion();
        // A blocked plan means the whole row is FAILED — no orders, no specialty, no tests leak.
        assertThat(saved.getStatus()).isEqualTo(CareSuggestionStatus.FAILED);
        assertThat(saved.getSuggestedReferralSpecialty()).isNull();
        assertThat(saved.getSuggestedTests()).isNull();
        verifyNoInteractions(orderSuggestionGuardrail);
    }

    @Test
    void triage_passesRetrievedExemplarsIntoModelContext() {
        when(planRetriever.retrieveExemplars(anyString(), anyInt()))
                .thenReturn(List.of("Approved plan: supportive care and safety-net advice."));
        stubTriage(new TriageResult(UrgencyLevel.ROUTINE, "Routine."));

        aiTriageService.triageEpisode(episodeId);

        verify(aiTriageDao).analyze(argThat(ctx ->
                ctx.approvedPlanExamples().size() == 1
                        && ctx.approvedPlanExamples().get(0).contains("supportive care")));
    }
}
