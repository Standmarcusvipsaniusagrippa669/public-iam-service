package com.valiantech.core.iam.user.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private UUID id;

    @Column(name = "full_name")
    private String fullName;

    @Column(unique = true)
    private String email;

    @Column(name = "password_hash", columnDefinition = "text")
    private String passwordHash;

    @Column(name = "email_validated")
    private Boolean emailValidated;

    @Column(name = "last_password_change")
    private Instant lastPasswordChange;

    @Column(name = "must_change_password")
    private Boolean mustChangePassword;

    private String status;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
