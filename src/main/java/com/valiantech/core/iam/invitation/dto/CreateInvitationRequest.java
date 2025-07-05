package com.valiantech.core.iam.invitation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateInvitationRequest(
        @NotNull UUID companyId,

        @NotBlank String role,

        @NotBlank @Email String invitedEmail,

        @NotNull UUID invitedBy
) {}
