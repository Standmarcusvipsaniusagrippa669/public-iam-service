package com.valiantech.core.iam.auth.dto;

public record LoginRequest(
        String email,
        String password
) {}
