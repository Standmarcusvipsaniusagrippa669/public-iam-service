package com.valiantech.core.iam.company.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    private UUID id;

    @Column(length = 10)
    private String rut;

    private String businessName;

    private String tradeName;

    private String activity;

    private String address;

    private String commune;

    private String region;

    private String email;

    private String phone;

    @Column(columnDefinition = "text")
    private String logoUrl;

    private String status;

    private Instant createdAt;

    private Instant updatedAt;
}
