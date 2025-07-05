package com.valiantech.core.iam.invitation.dto;

import com.valiantech.core.iam.usercompany.model.UserCompanyRole;

import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(
        UUID id,
        String invitedEmail,
        UUID companyId,
        UserCompanyRole role,
        UUID invitedBy,
        String invitationToken,
        String registrationUrl,

        String status,
        Instant expiresAt,
        Instant acceptedAt,
        Instant createdAt,
        Instant updatedAt
) {}
