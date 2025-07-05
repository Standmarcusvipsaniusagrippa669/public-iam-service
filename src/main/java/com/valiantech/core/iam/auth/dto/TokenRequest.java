package com.valiantech.core.iam.auth.dto;

import java.util.UUID;

public record TokenRequest(
        String email,
        UUID companyId,
        String loginTicket
) {}
