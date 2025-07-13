package com.valiantech.core.iam.audit.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.valiantech.core.iam.audit.model.AuditAction;
import com.valiantech.core.iam.audit.model.AuditLogEntry;
import com.valiantech.core.iam.audit.model.Metadata;
import com.valiantech.core.iam.audit.repository.UserAuditLogRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAuditLogServiceTest {

    @Mock UserAuditLogRepository repository;
    @Mock ObjectMapper objectMapper;

    @InjectMocks
    UserAuditLogService service;

    @Test
    @DisplayName("Debe guardar registro de auditoría con metadata serializada")
    void shouldSaveAuditLogWithSerializedMetadata() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        AuditLogEntry entry = AuditLogEntry.builder()
                .userId(userId)
                .companyId(companyId)
                .action(AuditAction.LOGIN_SUCCESS)
                .metadata(new Metadata("desct", Map.of("key", "val")))
                .ipAddress("127.0.0.1")
                .userAgent("JUnit")
                .build();

        when(objectMapper.writeValueAsString(entry.getMetadata())).thenReturn("{\"description\": \"desct\", \"object\": {\"key\":\"val\"}}");

        service.logAsync(entry);

        verify(objectMapper).setSerializationInclusion(JsonInclude.Include.NON_NULL);
        verify(objectMapper).writeValueAsString(entry.getMetadata());
        verify(repository).save(argThat(log ->
                log.getUserId().equals(userId)
                        && log.getCompanyId().equals(companyId)
                        && log.getAction().equals(AuditAction.LOGIN_SUCCESS)
                        && log.getMetadata().equals("{\"description\": \"desct\", \"object\": {\"key\":\"val\"}}")
                        && log.getIpAddress().equals("127.0.0.1")
                        && log.getUserAgent().equals("JUnit")
        ));
    }

    @Test
    @DisplayName("Debe guardar registro de auditoría aunque falle serialización de metadata")
    void shouldSaveAuditLogIfSerializationFails() throws Exception {
        AuditLogEntry entry = AuditLogEntry.builder()
                .userId(UUID.randomUUID())
                .companyId(UUID.randomUUID())
                .action(AuditAction.LOGOUT)
                .metadata(new Metadata("desct", Map.of("key", "val")))
                .build();

        when(objectMapper.writeValueAsString(entry.getMetadata()))
                .thenThrow(new JsonProcessingException("mock") {});

        service.logAsync(entry);

        verify(objectMapper).setSerializationInclusion(JsonInclude.Include.NON_NULL);
        verify(objectMapper).writeValueAsString(entry.getMetadata());
        verify(repository).save(argThat(log ->
                log.getAction().equals(AuditAction.LOGOUT) &&
                        log.getMetadata() == null
        ));
    }
}
