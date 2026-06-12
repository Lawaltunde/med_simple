package com.medsimple.patient.repository;

import com.medsimple.patient.entity.PatientClinicRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PatientClinicRegistrationRepository extends JpaRepository<PatientClinicRegistration, UUID> {

    List<PatientClinicRegistration> findByPatientId(UUID patientId);

    boolean existsByPatientIdAndClinicId(UUID patientId, UUID clinicId);
}
