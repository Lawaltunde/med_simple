package com.medsimple.referral.service;

import com.medsimple.audit.service.AuditService;
import com.medsimple.auth.security.AccessGuard;
import com.medsimple.careplan.entity.CarePlan;
import com.medsimple.careplan.repository.CarePlanRepository;
import com.medsimple.clinic.entity.Clinic;
import com.medsimple.referral.dao.*;
import com.medsimple.referral.dto.ConfirmSpecialtyRequest;
import com.medsimple.referral.dto.CreateReferralRequest;
import com.medsimple.referral.dto.ReferralResponse;
import com.medsimple.referral.entity.Referral;
import com.medsimple.referral.entity.ReferralDeliveryMethod;
import com.medsimple.referral.entity.ReferralStatus;
import com.medsimple.referral.entity.ReferralUrgency;
import com.medsimple.referral.exception.InvalidReferralStateException;
import com.medsimple.referral.repository.ReferralRepository;
import com.medsimple.shared.entity.User;
import com.medsimple.shared.entity.UserRole;
import com.medsimple.symptom.entity.EpisodeStatus;
import com.medsimple.symptom.entity.SymptomEpisode;
import com.medsimple.symptom.repository.SymptomEpisodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReferralServiceTest {

    @Mock ReferralRepository referralRepository;
    @Mock CarePlanRepository carePlanRepository;
    @Mock SymptomEpisodeRepository episodeRepository;
    @Mock SpecialtySuggestionDao specialtySuggestionDao;
    @Mock ReferralDeliveryResolver deliveryResolver;
    @Mock ReferralDeliveryDao deliveryDao;
    @Mock AuditService auditService;
    @Mock AccessGuard accessGuard;

    @InjectMocks ReferralService referralService;

    private User patient;
    private User physician;
    private Clinic clinic;
    private CarePlan carePlan;
    private SymptomEpisode episode;
    private UUID carePlanId;

    @BeforeEach
    void setUp() {
        carePlanId = UUID.randomUUID();
        patient = User.builder().id(UUID.randomUUID()).role(UserRole.PATIENT)
                .firstName("Jane").lastName("Doe").email("jane@x.com").build();
        physician = User.builder().id(UUID.randomUUID()).role(UserRole.PHYSICIAN)
                .firstName("Doc").lastName("S").email("doc@x.com").build();
        clinic = Clinic.builder().id(UUID.randomUUID()).name("Clinic").build();
        carePlan = CarePlan.builder().id(carePlanId).patient(patient).physician(physician)
                .clinic(clinic).createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build();
        episode = SymptomEpisode.builder().id(UUID.randomUUID()).patient(patient)
                .assignedClinic(clinic).assignedPhysician(physician).status(EpisodeStatus.IN_PROGRESS)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build();
    }

    private Referral draft(String aiSpecialty, String confirmed) {
        return Referral.builder()
                .id(UUID.randomUUID()).episode(episode).carePlan(carePlan).patient(patient)
                .referringPhysician(physician).aiSuggestedSpecialty(aiSpecialty)
                .confirmedSpecialty(confirmed).clinicalSummary("chest pain")
                .urgency(ReferralUrgency.ROUTINE).status(ReferralStatus.DRAFT)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build();
    }

    // ── create + AI suggestion ──────────────────────────────────────────────────

    @Test
    void create_fromEpisode_runsSpecialtySuggestion_noCarePlanNeeded() {
        CreateReferralRequest request = new CreateReferralRequest(
                episode.getId(), "Recurrent chest pain on exertion", ReferralUrgency.URGENT);

        when(episodeRepository.findById(episode.getId())).thenReturn(Optional.of(episode));
        when(specialtySuggestionDao.suggest("Recurrent chest pain on exertion"))
                .thenReturn(new SpecialtySuggestion("Cardiology", "Cardiac presentation"));
        when(referralRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReferralResponse response = referralService.createReferral(request, "127.0.0.1");

        assertThat(response.episodeId()).isEqualTo(episode.getId());
        assertThat(response.aiSuggestedSpecialty()).isEqualTo("Cardiology");
        assertThat(response.status()).isEqualTo(ReferralStatus.DRAFT);
        assertThat(response.carePlanId()).isNull(); // referred without a care plan
        verify(specialtySuggestionDao).suggest("Recurrent chest pain on exertion");
    }

    @Test
    void create_episodeNotAssignedToPhysician_throws() {
        episode.setAssignedPhysician(null);
        when(episodeRepository.findById(episode.getId())).thenReturn(Optional.of(episode));

        assertThatThrownBy(() -> referralService.createReferral(
                new CreateReferralRequest(episode.getId(), "chest pain", ReferralUrgency.ROUTINE), null))
                .isInstanceOf(com.medsimple.shared.exception.BusinessRuleException.class);
        verify(referralRepository, never()).save(any());
        verifyNoInteractions(specialtySuggestionDao);
    }

    // ── confirm specialty (physician override) ──────────────────────────────────

    @Test
    void confirmSpecialty_overridesAiSuggestion() {
        Referral ref = draft("Cardiology", null);
        when(referralRepository.findById(ref.getId())).thenReturn(Optional.of(ref));
        when(referralRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReferralResponse response = referralService.confirmSpecialty(ref.getId(),
                new ConfirmSpecialtyRequest("Respirology"), "127.0.0.1");

        assertThat(response.confirmedSpecialty()).isEqualTo("Respirology");
        assertThat(response.aiSuggestedSpecialty()).isEqualTo("Cardiology");
    }

    // ── submit + delivery ───────────────────────────────────────────────────────

    @Test
    void submit_unconfirmed_usesAiSuggestionAsOperativeSpecialty() {
        Referral ref = draft("Cardiology", null);
        when(referralRepository.findById(ref.getId())).thenReturn(Optional.of(ref));
        when(deliveryResolver.resolve(clinic.getId())).thenReturn(deliveryDao);
        when(deliveryDao.deliver(ref)).thenReturn(new ReferralDeliveryResult(
                ReferralStatus.SUBMITTED, ReferralDeliveryMethod.PDF_PRINT, null, "storage://ref.pdf"));
        when(referralRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReferralResponse response = referralService.submitReferral(ref.getId(), "127.0.0.1");

        assertThat(ref.getConfirmedSpecialty()).isEqualTo("Cardiology");
        assertThat(response.status()).isEqualTo(ReferralStatus.SUBMITTED);
        assertThat(response.deliveryMethod()).isEqualTo(ReferralDeliveryMethod.PDF_PRINT);
        assertThat(response.documentUrl()).isEqualTo("storage://ref.pdf");
    }

    @Test
    void submit_oceanPath_setsExternalReferralId() {
        Referral ref = draft("Cardiology", "Cardiology");
        when(referralRepository.findById(ref.getId())).thenReturn(Optional.of(ref));
        when(deliveryResolver.resolve(clinic.getId())).thenReturn(deliveryDao);
        when(deliveryDao.deliver(ref)).thenReturn(new ReferralDeliveryResult(
                ReferralStatus.SUBMITTED, ReferralDeliveryMethod.OCEAN_EREFERRAL, "OCEAN-ABC123", null));
        when(referralRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReferralResponse response = referralService.submitReferral(ref.getId(), "127.0.0.1");

        assertThat(response.deliveryMethod()).isEqualTo(ReferralDeliveryMethod.OCEAN_EREFERRAL);
        assertThat(response.externalReferralId()).isEqualTo("OCEAN-ABC123");
        assertThat(response.documentUrl()).isNull();
    }

    @Test
    void submit_nonDraft_throws() {
        Referral ref = draft("Cardiology", "Cardiology");
        ref.setStatus(ReferralStatus.SUBMITTED);
        when(referralRepository.findById(ref.getId())).thenReturn(Optional.of(ref));

        assertThatThrownBy(() -> referralService.submitReferral(ref.getId(), null))
                .isInstanceOf(InvalidReferralStateException.class);
    }

    // ── lifecycle: accept -> complete ───────────────────────────────────────────

    @Test
    void accept_submittedBecomesAccepted() {
        Referral ref = draft("Cardiology", "Cardiology");
        ref.setStatus(ReferralStatus.SUBMITTED);
        when(referralRepository.findById(ref.getId())).thenReturn(Optional.of(ref));
        when(referralRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(referralService.acceptReferral(ref.getId(), "127.0.0.1").status())
                .isEqualTo(ReferralStatus.ACCEPTED);
    }

    @Test
    void complete_acceptedBecomesCompleted() {
        Referral ref = draft("Cardiology", "Cardiology");
        ref.setStatus(ReferralStatus.ACCEPTED);
        when(referralRepository.findById(ref.getId())).thenReturn(Optional.of(ref));
        when(referralRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(referralService.completeReferral(ref.getId(), "127.0.0.1").status())
                .isEqualTo(ReferralStatus.COMPLETED);
    }

    @Test
    void cancel_completedReferral_throws() {
        Referral ref = draft("Cardiology", "Cardiology");
        ref.setStatus(ReferralStatus.COMPLETED);
        when(referralRepository.findById(ref.getId())).thenReturn(Optional.of(ref));

        assertThatThrownBy(() -> referralService.cancelReferral(ref.getId(), null))
                .isInstanceOf(InvalidReferralStateException.class);
    }

    @Test
    void getPatientReferrals_checksOwnershipThenReturns() {
        UUID pid = patient.getId();
        when(referralRepository.findByPatientIdOrderByCreatedAtDesc(pid))
                .thenReturn(List.of(draft("Cardiology", "Cardiology")));

        List<ReferralResponse> out = referralService.getPatientReferrals(pid);

        assertThat(out).hasSize(1);
        verify(accessGuard).requirePatientSelfOrStaff(pid);
    }

    @Test
    void getPatientReferrals_deniedForAnotherPatient() {
        UUID pid = patient.getId();
        doThrow(new AccessDeniedException("not your referrals"))
                .when(accessGuard).requirePatientSelfOrStaff(pid);

        assertThatThrownBy(() -> referralService.getPatientReferrals(pid))
                .isInstanceOf(AccessDeniedException.class);
        verify(referralRepository, never()).findByPatientIdOrderByCreatedAtDesc(any());
    }
}
