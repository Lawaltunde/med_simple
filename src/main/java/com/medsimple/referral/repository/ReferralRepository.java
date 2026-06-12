package com.medsimple.referral.repository;

import com.medsimple.referral.entity.Referral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, UUID> {

    List<Referral> findByCarePlanIdOrderByCreatedAtDesc(UUID carePlanId);

    List<Referral> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
}
