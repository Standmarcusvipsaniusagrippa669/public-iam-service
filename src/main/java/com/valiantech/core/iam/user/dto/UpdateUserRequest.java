package com.valiantech.core.iam.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 100)
        String fullName,

        @Email
        String email,

        String status,

        Boolean mustChangePassword
) {}
