package com.medsimple.patient.repository;

import com.medsimple.patient.entity.PatientConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PatientConsentRepository extends JpaRepository<PatientConsent, UUID> {

    List<PatientConsent> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
}
