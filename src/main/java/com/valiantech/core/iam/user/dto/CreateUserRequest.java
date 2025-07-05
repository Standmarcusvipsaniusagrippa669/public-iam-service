package com.valiantech.core.iam.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 100)
        String fullName,

        @NotBlank @Email
        String email,

        @NotBlank @Size(min = 8)
        String password
) {}
