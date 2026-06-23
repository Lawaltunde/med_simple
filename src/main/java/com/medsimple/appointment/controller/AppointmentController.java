package com.medsimple.appointment.controller;

import com.medsimple.appointment.dto.AppointmentResponse;
import com.medsimple.appointment.dto.CreateAppointmentRequest;
import com.medsimple.appointment.service.AppointmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PreAuthorize("hasRole('PHYSICIAN')")
    @PostMapping("/api/v1/appointments")
    public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody CreateAppointmentRequest request,
                                                      HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.createAppointment(request, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/api/v1/patients/{patientId}/appointments")
    public ResponseEntity<List<AppointmentResponse>> getForPatient(@PathVariable UUID patientId) {
        return ResponseEntity.ok(appointmentService.getPatientAppointments(patientId));
    }

    @GetMapping("/api/v1/clinics/{clinicId}/appointments")
    public ResponseEntity<List<AppointmentResponse>> getForClinic(@PathVariable UUID clinicId) {
        return ResponseEntity.ok(appointmentService.getClinicAppointments(clinicId));
    }

    // Standard .ics so the patient/physician can add it to Google / Apple / Outlook themselves.
    @GetMapping("/api/v1/appointments/{id}/calendar.ics")
    public ResponseEntity<String> calendar(@PathVariable UUID id) {
        String ics = appointmentService.getIcs(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"medsimple-appointment.ics\"")
                .body(ics);
    }
}
