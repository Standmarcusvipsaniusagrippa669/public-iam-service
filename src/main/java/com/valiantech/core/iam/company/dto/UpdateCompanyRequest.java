package com.valiantech.core.iam.company.dto;

public record UpdateCompanyRequest(
        String businessName,
        String tradeName,
        String activity,
        String address,
        String commune,
        String region,
        String email,
        String phone,
        String logoUrl,
        String status
) {}
