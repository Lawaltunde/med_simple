package com.medsimple.patient.repository;

import com.medsimple.shared.entity.User;
import com.medsimple.shared.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<User, UUID> {

    Optional<User> findByIdAndRole(UUID id, UserRole role);

    boolean existsByEmail(String email);
}
