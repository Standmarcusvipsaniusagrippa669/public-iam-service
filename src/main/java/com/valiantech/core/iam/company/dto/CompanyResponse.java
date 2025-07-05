package com.valiantech.core.iam.company.dto;

import java.time.Instant;
import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String rut,
        String businessName,
        String tradeName,
        String activity,
        String address,
        String commune,
        String region,
        String email,
        String phone,
        String logoUrl,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
