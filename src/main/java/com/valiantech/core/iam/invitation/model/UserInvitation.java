package com.valiantech.core.iam.invitation.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInvitation {

    @Id
    private UUID id;

    private String invitedEmail;

    @Column(nullable = false)
    private UUID companyId;

    private String role;

    private UUID invitedBy;

    private String invitationToken;

    private String status;

    @Column(name = "registration_url", columnDefinition = "text")
    private String registrationUrl;

    private Instant expiresAt;

    private Instant acceptedAt;

    private Instant createdAt;

    private Instant updatedAt;
}
