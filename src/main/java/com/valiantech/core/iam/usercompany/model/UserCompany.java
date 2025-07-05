package com.valiantech.core.iam.usercompany.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_companies", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "company_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCompany {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID companyId;

    private String role;

    private UUID invitedBy;

    private String status; // active, invited, blocked, disabled

    private Instant createdAt;

    private Instant updatedAt;
}
