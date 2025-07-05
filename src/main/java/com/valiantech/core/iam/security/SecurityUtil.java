package com.valiantech.core.iam.security;

import com.valiantech.core.iam.exception.ForbiddenException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.UUID;

public class SecurityUtil {
    public static UUID getCompanyIdFromContext() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null)
            throw new ForbiddenException("No authentication context found"); // Usa tu excepción o 401/403

        Object details = auth.getDetails();
        if (!(details instanceof Map<?,?> detailsMap))
            throw new ForbiddenException("No company context in token");

        Object companyIdObj = detailsMap.get("companyId");
        if (companyIdObj == null)
            throw new ForbiddenException("No company context in token");

        try {
            return UUID.fromString(companyIdObj.toString());
        } catch (IllegalArgumentException e) {
            throw new ForbiddenException("Invalid companyId in token");
        }
    }
}
