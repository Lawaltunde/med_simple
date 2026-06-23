package com.medsimple.appointment.repository;

import com.medsimple.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByPatientIdOrderByScheduledAtAsc(UUID patientId);

    List<Appointment> findByClinicIdOrderByScheduledAtAsc(UUID clinicId);
}
