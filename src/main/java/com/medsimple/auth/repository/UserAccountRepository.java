package com.medsimple.auth.repository;

import com.medsimple.shared.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

// Auth-scoped lookups against the users table (login by email, current-user fetch).
@Repository
public interface UserAccountRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
}
