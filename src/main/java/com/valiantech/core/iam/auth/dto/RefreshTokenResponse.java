package com.valiantech.core.iam.auth.dto;

public record RefreshTokenResponse(
        String authToken,
        String refreshToken
) {}