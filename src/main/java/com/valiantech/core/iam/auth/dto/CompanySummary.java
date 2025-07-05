package com.valiantech.core.iam.auth.dto;

import java.util.UUID;

public record CompanySummary(
        UUID companyId,
        String companyName,
        String role
) {}