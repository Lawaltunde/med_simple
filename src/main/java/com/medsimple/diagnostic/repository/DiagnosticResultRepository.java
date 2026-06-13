package com.medsimple.diagnostic.repository;

import com.medsimple.diagnostic.entity.DiagnosticResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DiagnosticResultRepository extends JpaRepository<DiagnosticResult, UUID> {

    // Physician view — every result for the patient, released or not.
    List<DiagnosticResult> findByPatientIdOrderByReceivedAtDesc(UUID patientId);

    // Patient view — ONLY released results. The PHIPA gate, expressed as a query.
    List<DiagnosticResult> findByPatientIdAndReleasedToPatientAtIsNotNullOrderByReceivedAtDesc(UUID patientId);
}
