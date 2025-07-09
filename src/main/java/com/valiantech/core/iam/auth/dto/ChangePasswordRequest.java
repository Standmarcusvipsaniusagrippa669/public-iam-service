package com.valiantech.core.iam.auth.dto;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword
) {}