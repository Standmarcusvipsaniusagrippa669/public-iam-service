package com.valiantech.core.iam.auth.dto;

import com.valiantech.core.iam.user.dto.UserResponse;

public record LoginResponse(
        String token, // JWT
        String refreshToken, // (opcional)
        UserResponse user
) {}