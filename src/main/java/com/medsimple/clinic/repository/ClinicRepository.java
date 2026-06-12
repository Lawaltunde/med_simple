package com.medsimple.clinic.repository;

import com.medsimple.clinic.entity.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ClinicRepository extends JpaRepository<Clinic, UUID> {

    boolean existsByEmail(String email);
}
