package com.medsimple.patient.repository;

import com.medsimple.patient.entity.PatientHealthCoverage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientHealthCoverageRepository extends JpaRepository<PatientHealthCoverage, UUID> {

    List<PatientHealthCoverage> findByPatientIdAndActiveTrue(UUID patientId);

    Optional<PatientHealthCoverage> findByIdAndPatientId(UUID id, UUID patientId);
}
