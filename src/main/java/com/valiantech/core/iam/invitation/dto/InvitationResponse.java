package com.valiantech.core.iam.invitation.dto;

import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(
        UUID id,
        String invitedEmail,
        UUID companyId,
        String role,
        UUID invitedBy,
        String invitationToken,
        String registrationUrl,

        String status,
        Instant expiresAt,
        Instant acceptedAt,
        Instant createdAt,
        Instant updatedAt
) {}
