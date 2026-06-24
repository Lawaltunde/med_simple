package com.medsimple.diagnostic.repository;

import com.medsimple.diagnostic.entity.DiagnosticRequisition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DiagnosticRequisitionRepository extends JpaRepository<DiagnosticRequisition, UUID> {

    List<DiagnosticRequisition> findByCarePlanIdOrderByCreatedAtDesc(UUID carePlanId);

    List<DiagnosticRequisition> findByEpisodeIdOrderByCreatedAtDesc(UUID episodeId);

    List<DiagnosticRequisition> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
}
