package com.valiantech.core.iam.auth.service;

import com.valiantech.core.iam.auth.model.UserLogin;
import com.valiantech.core.iam.auth.repository.UserLoginRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for managing user login events.
 * Provides methods to record login attempts and query login history.
 */
@Service
@RequiredArgsConstructor
public class UserLoginService {

    private final UserLoginRepository userLoginRepository;

    /**
     * Records a user login attempt with relevant metadata.
     *
     * @param userId        UUID of the user
     * @param ipAddress     IP address of the client
     * @param userAgent     User-Agent string of the client
     * @param success       True if login succeeded, false otherwise
     * @param failureReason Optional reason for failure (nullable)
     */
    @Async
    @Transactional
    public void recordLoginAttempt(UUID userId, UUID companyId, String ipAddress, String userAgent, boolean success, String failureReason) {
        UserLogin login = UserLogin.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .loginAt(Instant.now())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(success)
                .failureReason(failureReason)
                .companyId(companyId)
                .build();

        userLoginRepository.save(login);
    }
}
