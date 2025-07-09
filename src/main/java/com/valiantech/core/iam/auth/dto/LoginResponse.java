package com.valiantech.core.iam.auth.dto;

import com.valiantech.core.iam.user.dto.UserResponse;

import java.util.UUID;

public record LoginResponse(
        String authToken,
        String refreshToken,
        UserResponse user,
        UUID companyId,
        String role
) {}