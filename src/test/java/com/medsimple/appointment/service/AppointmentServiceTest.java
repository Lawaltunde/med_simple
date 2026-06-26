package com.medsimple.appointment.service;

import com.medsimple.appointment.dto.AppointmentResponse;
import com.medsimple.appointment.dto.CreateAppointmentRequest;
import com.medsimple.appointment.entity.Appointment;
import com.medsimple.appointment.repository.AppointmentRepository;
import com.medsimple.audit.service.AuditService;
import com.medsimple.auth.security.AccessGuard;
import com.medsimple.clinic.entity.Clinic;
import com.medsimple.notification.entity.NotificationType;
import com.medsimple.notification.service.NotificationService;
import com.medsimple.patient.repository.PatientRepository;
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
class AppointmentServiceTest {

    @Mock AppointmentRepository appointmentRepository;
    @Mock SymptomEpisodeRepository episodeRepository;
    @Mock PatientRepository userRepository;
    @Mock AuditService auditService;
    @Mock NotificationService notificationService;
    @Mock AccessGuard accessGuard;

    @InjectMocks AppointmentService appointmentService;

    private User patient;
    private User physician;
    private Clinic clinic;
    private SymptomEpisode episode;
    private UUID episodeId;
    private UUID physicianId;

    @BeforeEach
    void setUp() {
        episodeId = UUID.randomUUID();
        physicianId = UUID.randomUUID();
        patient = User.builder().id(UUID.randomUUID()).role(UserRole.PATIENT)
                .firstName("Jane").lastName("Doe").email("jane@x.com").build();
        physician = User.builder().id(physicianId).role(UserRole.PHYSICIAN)
                .firstName("Dana").lastName("Okafor").email("doc@x.com").build();
        clinic = Clinic.builder().id(UUID.randomUUID()).name("Demo Clinic").build();
        episode = SymptomEpisode.builder().id(episodeId).patient(patient).assignedClinic(clinic)
                .status(EpisodeStatus.IN_PROGRESS)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build();
    }

    @Test
    void create_success_booksAndNotifiesPatient() {
        var request = new CreateAppointmentRequest(episodeId, physicianId,
                OffsetDateTime.now().plusDays(3), 20, "Follow-up on throat");
        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
        when(userRepository.findByIdAndRole(physicianId, UserRole.PHYSICIAN)).thenReturn(Optional.of(physician));
        when(appointmentRepository.save(any())).thenAnswer(inv -> {
            Appointment a = inv.getArgument(0); a.setId(UUID.randomUUID()); return a;
        });

        AppointmentResponse resp = appointmentService.createAppointment(request, "127.0.0.1");

        assertThat(resp.patientId()).isEqualTo(patient.getId());
        assertThat(resp.clinicName()).isEqualTo("Demo Clinic");
        assertThat(resp.physicianName()).isEqualTo("Dana Okafor");
        assertThat(resp.durationMinutes()).isEqualTo(20);
        verify(accessGuard).requireStaff();
        verify(notificationService).notify(eq(patient.getId()), eq(NotificationType.APPOINTMENT_REMINDER),
                anyString(), anyString());
        verify(auditService).log(eq(physicianId), eq("APPOINTMENT_BOOKED"), eq("appointments"), any(), any());
    }

    @Test
    void create_episodeWithoutClinic_throws() {
        episode.setAssignedClinic(null);
        var request = new CreateAppointmentRequest(episodeId, physicianId, OffsetDateTime.now().plusDays(1), 15, null);
        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));

        assertThatThrownBy(() -> appointmentService.createAppointment(request, null))
                .isInstanceOf(com.medsimple.shared.exception.BusinessRuleException.class);
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void getPatientAppointments_checksOwnershipThenReturns() {
        UUID pid = patient.getId();
        when(appointmentRepository.findByPatientIdOrderByScheduledAtAsc(pid)).thenReturn(List.of(
                Appointment.builder().id(UUID.randomUUID()).patient(patient).physician(physician).clinic(clinic)
                        .scheduledAt(OffsetDateTime.now().plusDays(2)).durationMinutes(15).build()));

        List<AppointmentResponse> out = appointmentService.getPatientAppointments(pid);

        assertThat(out).hasSize(1);
        verify(accessGuard).requirePatientSelfOrStaff(pid);
    }

    @Test
    void getPatientAppointments_deniedForAnotherPatient() {
        UUID pid = patient.getId();
        doThrow(new AccessDeniedException("nope")).when(accessGuard).requirePatientSelfOrStaff(pid);

        assertThatThrownBy(() -> appointmentService.getPatientAppointments(pid))
                .isInstanceOf(AccessDeniedException.class);
        verify(appointmentRepository, never()).findByPatientIdOrderByScheduledAtAsc(any());
    }

    @Test
    void getIcs_gatedToPatientSelfOrStaff_andReturnsCalendar() {
        UUID id = UUID.randomUUID();
        Appointment a = Appointment.builder().id(id).patient(patient).physician(physician).clinic(clinic)
                .scheduledAt(OffsetDateTime.now().plusDays(1)).durationMinutes(15).reason("Check-in").build();
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(a));

        String ics = appointmentService.getIcs(id);

        assertThat(ics).contains("BEGIN:VCALENDAR");
        verify(accessGuard).requirePatientSelfOrStaff(patient.getId());
    }
}
