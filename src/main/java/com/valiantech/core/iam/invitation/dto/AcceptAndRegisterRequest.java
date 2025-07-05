package com.valiantech.core.iam.invitation.dto;

import jakarta.validation.constraints.NotBlank;

public record AcceptAndRegisterRequest(
        @NotBlank String token,
        @NotBlank String fullName,
        @NotBlank String password
) {}
