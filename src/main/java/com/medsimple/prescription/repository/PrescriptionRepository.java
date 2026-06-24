package com.medsimple.prescription.repository;

import com.medsimple.prescription.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {

    List<Prescription> findByCarePlanIdOrderByCreatedAtDesc(UUID carePlanId);

    List<Prescription> findByEpisodeIdOrderByCreatedAtDesc(UUID episodeId);

    List<Prescription> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
}
