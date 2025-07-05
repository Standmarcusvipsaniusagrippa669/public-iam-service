package com.valiantech.core.iam.user.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String email,
        Boolean emailValidated,
        String status,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
) {}
