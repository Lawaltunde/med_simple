package com.medsimple.careplan.repository;

import com.medsimple.careplan.entity.CarePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CarePlanRepository extends JpaRepository<CarePlan, UUID> {

    List<CarePlan> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    List<CarePlan> findByEpisodeId(UUID episodeId);
}
