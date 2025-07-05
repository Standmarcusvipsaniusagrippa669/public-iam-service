package com.valiantech.core.iam.auth.dto;

import com.valiantech.core.iam.user.dto.UserResponse;

import java.util.List;

public record AssociatedCompanies(
        UserResponse user,
        List<CompanySummary> companies,
        String loginTicket
) {}