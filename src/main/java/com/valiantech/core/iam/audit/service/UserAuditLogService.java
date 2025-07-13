package com.valiantech.core.iam.audit.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.valiantech.core.iam.audit.model.AuditLogEntry;
import com.valiantech.core.iam.audit.model.UserAuditLog;
import com.valiantech.core.iam.audit.repository.UserAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Servicio encargado de registrar de forma asíncrona los eventos de auditoría de usuarios en la plataforma.
 * <ul>
 *   <li>Convierte los metadatos del evento a JSON (ignorando nulos).</li>
 *   <li>Guarda la acción de usuario en la base de datos junto con detalles como IP, agente, recurso, etc.</li>
 *   <li>En caso de error al serializar los metadatos, continúa y registra el evento sin ellos (log de warning).</li>
 *   <li>El método es asíncrono para no afectar el flujo principal de la aplicación.</li>
 * </ul>
 * <b>Notas:</b>
 * <ul>
 *   <li>El evento incluye marca de tiempo y toda la información relevante para trazabilidad.</li>
 *   <li>Se recomienda auditar acciones críticas y sensibles para cumplimiento y debugging.</li>
 * </ul>
 * @author Ian Cardenas
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class UserAuditLogService {

    private final UserAuditLogRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * Registra un evento de auditoría con los datos proporcionados.
     *
     * @param entry      Objeto de log.
     */
    @Async
    public void logAsync(AuditLogEntry entry) {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        String metadataAsString = null;
        try {
            metadataAsString = objectMapper.writeValueAsString(entry.getMetadata());
        } catch (JsonProcessingException ex) {
            log.warn("Cannot write medatada as string: {}", ex.getMessage());
        }

        repository.save(UserAuditLog.builder()
                .id(UUID.randomUUID())
                .userId(entry.getUserId())
                .companyId(entry.getCompanyId())
                .action(entry.getAction())
                .targetUserId(entry.getTargetUserId())
                .resourceType(entry.getResourceType())
                .resourceId(entry.getResourceId())
                .metadata(metadataAsString)
                .cookies(entry.getCookies())
                .actionAt(Instant.now())
                .ipAddress(entry.getIpAddress())
                .userAgent(entry.getUserAgent())
                .createdAt(Instant.now())
                .build());
    }
}
