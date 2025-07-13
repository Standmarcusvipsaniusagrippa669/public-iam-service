package com.valiantech.core.iam.auth.repository;

import com.valiantech.core.iam.auth.model.UserPasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository to manage UserPasswordReset entities.
 */
public interface UserPasswordResetRepository extends JpaRepository<UserPasswordReset, UUID> {

    Optional<UserPasswordReset> findByResetToken(String resetToken);

    void deleteByExpiresAtBefore(Instant dateTime);
}
