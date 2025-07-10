package com.valiantech.core.iam.auth.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity that represents a user login event (success or failure).
 * Records metadata such as timestamp, IP address, user agent, and status.
 */
@Entity
@Table(name = "user_logins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLogin {

    @Id
    private UUID id;

    /**
     * User who attempted to login.
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * User who attempted to login.
     */
    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    /**
     * Timestamp when the login attempt occurred.
     */
    @Column(name = "login_at", nullable = false)
    private Instant loginAt;

    /**
     * IP address from where the login was attempted.
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User-Agent string of the client.
     */
    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    /**
     * Indicates if the login attempt was successful or not.
     */
    @Column(name = "successful", nullable = false)
    private boolean success;

    /**
     * Optional reason for failure (if success == false).
     */
    @Column(name = "fail_reason", columnDefinition = "text")
    private String failureReason;
}
