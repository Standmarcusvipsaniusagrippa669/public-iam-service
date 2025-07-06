package com.valiantech.core.iam.company.controller;

import com.valiantech.core.iam.company.dto.*;
import com.valiantech.core.iam.company.service.CompanyService;
import com.valiantech.core.iam.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
@Tag(name = "Company Management", description = "Operations for managing companies")
public class CompanyController {

    private final CompanyService companyService;

    @Operation(
            summary = "Create a new company",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Company successfully created"),
                    @ApiResponse(responseCode = "409", description = "Company with same RUT already exists")
            }
    )
    @PostMapping("/onboarding")
    public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CompanyOnboardingRequest request) {
        return ResponseEntity.ok(companyService.onboarding(request));
    }

    @Operation(
            summary = "Update an existing company",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Company updated"),
                    @ApiResponse(responseCode = "404", description = "Company not found")
            }
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<CompanyResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCompanyRequest request) {
        return ResponseEntity.ok(companyService.updateCompany(id, request));
    }

    @Operation(
            summary = "Obtener los datos de mi empresa (según el contexto del token)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Empresa encontrada"),
                    @ApiResponse(responseCode = "404", description = "Empresa no encontrada o no tienes acceso")
            }
    )
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','VIEWER')")
    public ResponseEntity<CompanyResponse> getMyCompany() {
        UUID companyId = SecurityUtil.getCompanyIdFromContext(); // Implementa este util para extraerlo del JWT
        return ResponseEntity.ok(companyService.getCompany(companyId));
    }
}
