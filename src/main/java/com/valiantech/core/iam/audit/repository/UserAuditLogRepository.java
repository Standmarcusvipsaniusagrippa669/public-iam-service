package com.valiantech.core.iam.audit.repository;

import com.valiantech.core.iam.audit.model.UserAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, UUID> {
    // Métodos personalizados si requieres consultas especiales
}