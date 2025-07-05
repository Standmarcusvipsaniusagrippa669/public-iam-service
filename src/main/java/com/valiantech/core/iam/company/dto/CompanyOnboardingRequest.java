package com.valiantech.core.iam.company.dto;

import com.valiantech.core.iam.user.dto.CreateUserRequest;

public record CompanyOnboardingRequest(
        CreateCompanyRequest company,
        CreateUserRequest owner
) {}
