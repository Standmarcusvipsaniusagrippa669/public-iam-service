package com.valiantech.core.iam.company.dto;

import com.valiantech.core.iam.company.model.CompanyStatus;

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
        CompanyStatus status
) {}
