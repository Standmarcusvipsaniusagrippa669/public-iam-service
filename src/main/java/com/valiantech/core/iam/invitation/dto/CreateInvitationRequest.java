package com.valiantech.core.iam.invitation.dto;

import com.valiantech.core.iam.usercompany.model.UserCompanyRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateInvitationRequest(
        @NotNull UserCompanyRole role,

        @NotBlank @Email String invitedEmail,

        @NotNull UUID invitedBy
) {}
