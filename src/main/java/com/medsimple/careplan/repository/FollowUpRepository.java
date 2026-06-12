package com.medsimple.careplan.repository;

import com.medsimple.careplan.entity.FollowUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FollowUpRepository extends JpaRepository<FollowUp, UUID> {

    List<FollowUp> findByCarePlanIdOrderByCreatedAtAsc(UUID carePlanId);

    Optional<FollowUp> findByIdAndCarePlanId(UUID id, UUID carePlanId);
}
