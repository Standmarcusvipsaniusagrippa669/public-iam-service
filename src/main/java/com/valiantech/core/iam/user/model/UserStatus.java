package com.valiantech.core.iam.user.model;

public enum UserStatus {
    ACTIVE,
    DISABLED, // no puede autenticarse en la plataforma
    SUSPENDED, // no puede autenticarse en la plataforma
    PENDING
}
