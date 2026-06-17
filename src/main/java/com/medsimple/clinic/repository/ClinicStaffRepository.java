package com.medsimple.clinic.repository;

import com.medsimple.clinic.entity.ClinicStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClinicStaffRepository extends JpaRepository<ClinicStaff, UUID> {

    List<ClinicStaff> findByClinicIdAndActiveTrue(UUID clinicId);

    // A staff member's active clinic memberships — drives "which queue is mine".
    List<ClinicStaff> findByUserIdAndActiveTrue(UUID userId);

    Optional<ClinicStaff> findByIdAndClinicId(UUID staffId, UUID clinicId);

    boolean existsByClinicIdAndUserId(UUID clinicId, UUID userId);
}
