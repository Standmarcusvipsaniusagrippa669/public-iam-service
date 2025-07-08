package com.valiantech.core.iam.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.valiantech.core.iam.user.dto.UserResponse;

import java.util.UUID;

public record LoginResponse(
        @JsonProperty("auth_token")
        String authToken,
        @JsonProperty("refresh_token")
        String refreshToken,
        UserResponse user,
        UUID companyId,
        String role
) {}