package com.valiantech.core.iam.auth.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "login_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginTicket {
    @Id
    private String id; // UUID como String
    private String email;
    private Instant expiresAt;
    private boolean used;
}
