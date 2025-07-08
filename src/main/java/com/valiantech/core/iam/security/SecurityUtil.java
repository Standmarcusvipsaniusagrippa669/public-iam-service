package com.valiantech.core.iam.security;

import com.valiantech.core.iam.exception.ForbiddenException;
import com.valiantech.core.iam.exception.GenericException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

public class SecurityUtil {
    SecurityUtil() {
        // empty
    }
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

    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new GenericException(String.format("SHA-256 algorithm not available: %s", e.getMessage()));
        }
    }
}
