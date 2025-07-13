package com.valiantech.core.iam.auth.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity to track password reset requests and their status.
 */
@Entity
@Table(name = "user_password_resets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPasswordReset {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "reset_token", nullable = false, unique = true)
    private String resetToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ResetPasswordStatus status; // Example values: REQUESTED, USED, EXPIRED, REVOKED

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
