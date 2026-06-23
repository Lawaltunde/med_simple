package com.medsimple.patient.repository;

import com.medsimple.patient.entity.ConsentType;
import com.medsimple.patient.entity.PatientConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientConsentRepository extends JpaRepository<PatientConsent, UUID> {

    List<PatientConsent> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    // The most recent event for a consent type decides the patient's current state
    // (rows are append-only; the latest grant/revoke wins).
    Optional<PatientConsent> findFirstByPatientIdAndConsentTypeOrderByCreatedAtDesc(
            UUID patientId, ConsentType consentType);
}
