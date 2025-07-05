package com.valiantech.core.iam.user.dto;

import com.valiantech.core.iam.user.model.User;
import com.valiantech.core.iam.user.model.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String email,
        Boolean emailValidated,
        UserStatus status,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
) {
    // Método factory
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getEmailValidated(),
                user.getStatus() != null ? user.getStatus() : null,
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
