package com.valiantech.core.iam.audit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;


/**
 * Data transfer object que representa una entrada para registrar un log de auditoría.
 * <p>
 * Contiene información contextual sobre la acción realizada, usuario, empresa, recursos afectados,
 * y datos del cliente como IP, User-Agent y cookies.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogEntry {

    /**
     * ID del usuario que realiza la acción.
     */
    private UUID userId;

    /**
     * ID de la empresa asociada a la acción.
     */
    private UUID companyId;

    /**
     * Acción auditada (ejemplo: LOGIN_SUCCESS, PASSWORD_CHANGE).
     */
    private AuditAction action;

    /**
     * ID del usuario objetivo sobre el que se realiza la acción (puede ser null).
     */
    private UUID targetUserId;

    /**
     * Tipo del recurso afectado por la acción (puede ser null).
     */
    private ResourceType resourceType;

    /**
     * ID del recurso afectado (puede ser null).
     */
    private UUID resourceId;

    /**
     * Metadata adicional en formato JSON o texto libre (puede ser null).
     */
    private Metadata metadata;

    /**
     * Dirección IP del cliente que realizó la acción.
     */
    private String ipAddress;

    /**
     * Cadena User-Agent del cliente que realizó la acción.
     */
    private String userAgent;

    /**
     * Cookies enviadas por el cliente en formato JSON o texto (puede ser null).
     */
    private String cookies;
}

