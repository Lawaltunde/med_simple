package com.medsimple.clinic.repository;

import com.medsimple.clinic.entity.CapabilityType;
import com.medsimple.clinic.entity.ClinicCapability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClinicCapabilityRepository extends JpaRepository<ClinicCapability, UUID> {

    List<ClinicCapability> findByClinicId(UUID clinicId);

    Optional<ClinicCapability> findByClinicIdAndCapability(UUID clinicId, CapabilityType capability);
}
