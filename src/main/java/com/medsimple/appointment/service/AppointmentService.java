package com.medsimple.appointment.service;

import com.medsimple.appointment.dto.AppointmentResponse;
import com.medsimple.appointment.dto.CreateAppointmentRequest;
import com.medsimple.appointment.entity.Appointment;
import com.medsimple.appointment.exception.AppointmentNotFoundException;
import com.medsimple.appointment.repository.AppointmentRepository;
import com.medsimple.audit.service.AuditService;
import com.medsimple.auth.security.AccessGuard;
import com.medsimple.notification.entity.NotificationType;
import com.medsimple.notification.service.NotificationService;
import com.medsimple.patient.exception.PatientNotFoundException;
import com.medsimple.patient.repository.PatientRepository;
import com.medsimple.shared.entity.User;
import com.medsimple.shared.entity.UserRole;
import com.medsimple.symptom.entity.SymptomEpisode;
import com.medsimple.symptom.exception.EpisodeNotFoundException;
import com.medsimple.symptom.repository.SymptomEpisodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEE d MMM, h:mm a");

    private final AppointmentRepository appointmentRepository;
    private final SymptomEpisodeRepository episodeRepository;
    private final PatientRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final AccessGuard accessGuard;

    @Transactional
    public AppointmentResponse createAppointment(CreateAppointmentRequest request, String ipAddress) {
        accessGuard.requireStaff(); // booking is a clinician action

        SymptomEpisode episode = episodeRepository.findById(request.episodeId())
                .orElseThrow(() -> new EpisodeNotFoundException(request.episodeId()));
        if (episode.getAssignedClinic() == null) {
            throw new IllegalStateException(
                    "Episode " + request.episodeId() + " has no assigned clinic; cannot book an appointment");
        }
        User physician = userRepository.findByIdAndRole(request.physicianId(), UserRole.PHYSICIAN)
                .orElseThrow(() -> new PatientNotFoundException(request.physicianId()));

        Appointment appointment = Appointment.builder()
                .patient(episode.getPatient())
                .physician(physician)
                .clinic(episode.getAssignedClinic())
                .episodeId(episode.getId())
                .scheduledAt(request.scheduledAt())
                .durationMinutes(request.durationMinutes() != null ? request.durationMinutes() : 15)
                .reason(request.reason())
                .build();
        Appointment saved = appointmentRepository.save(appointment);

        auditService.log(physician.getId(), "APPOINTMENT_BOOKED", "appointments", saved.getId(), ipAddress);
        notificationService.notify(episode.getPatient().getId(), NotificationType.APPOINTMENT_REMINDER,
                "Appointment booked",
                "Your physician booked an appointment for " + request.scheduledAt().format(WHEN)
                        + (request.reason() != null ? " — " + request.reason() : "") + ".");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getPatientAppointments(UUID patientId) {
        accessGuard.requirePatientSelfOrStaff(patientId);
        return appointmentRepository.findByPatientIdOrderByScheduledAtAsc(patientId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getClinicAppointments(UUID clinicId) {
        accessGuard.requireStaff();
        return appointmentRepository.findByClinicIdOrderByScheduledAtAsc(clinicId)
                .stream().map(this::toResponse).toList();
    }

    // The .ics body for an appointment, available to the patient (self) or staff.
    @Transactional(readOnly = true)
    public String getIcs(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));
        accessGuard.requirePatientSelfOrStaff(appointment.getPatient().getId());
        return AppointmentIcs.build(appointment);
    }

    private AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getPatient().getId(),
                a.getPhysician().getId(),
                a.getClinic().getId(),
                a.getEpisodeId(),
                a.getCarePlanId(),
                a.getScheduledAt(),
                a.getDurationMinutes(),
                a.getReason(),
                a.getStatus(),
                a.getClinic().getName(),
                a.getPhysician().getFirstName() + " " + a.getPhysician().getLastName(),
                a.getPatient().getFirstName() + " " + a.getPatient().getLastName(),
                a.getCreatedAt());
    }
}
