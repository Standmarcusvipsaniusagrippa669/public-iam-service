package com.valiantech.core.iam.usercompany.model;

public enum UserCompanyStatus {
    ACTIVE,
    DISABLED, // no puede autenticarse en la empresa
    INVITED,
    BLOCKED
}
