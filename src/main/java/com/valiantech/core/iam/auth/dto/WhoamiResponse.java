package com.valiantech.core.iam.auth.dto;

import java.util.UUID;

public record WhoamiResponse(
        UUID userId,
        String fullName,
        String email,
        boolean emailValidated,
        String status,
        UUID companyId,
        String companyName,
        String role
) {
}
