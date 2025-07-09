package com.valiantech.core.iam.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.valiantech.core.iam.audit.model.AuditAction;
import com.valiantech.core.iam.audit.model.AuditLogEntry;
import com.valiantech.core.iam.audit.model.UserAuditLog;
import com.valiantech.core.iam.audit.repository.UserAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

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
        String metadataAsString = null;
        try {
            metadataAsString = objectMapper.writeValueAsString(entry.getMetadata());
        } catch (JsonProcessingException ex) {
            log.warn("Cannot write medatada as string: {}", ex.getMessage());
        }

        UserAuditLog log = UserAuditLog.builder()
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
                .build();

        repository.save(log);
    }
}
