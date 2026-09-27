package com.sentinelap.repo;

import com.sentinelap.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, java.util.UUID> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
}
