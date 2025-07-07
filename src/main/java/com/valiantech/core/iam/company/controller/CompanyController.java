package com.valiantech.core.iam.company.controller;

import com.valiantech.core.iam.company.dto.*;
import com.valiantech.core.iam.company.service.CompanyService;
import com.valiantech.core.iam.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

    @SecurityRequirement(name = "bearerAuth")
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

    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Actualizar mi empresa (según el contexto del token)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Empresa actualizada"),
                    @ApiResponse(responseCode = "404", description = "Empresa no encontrada")
            }
    )
    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('OWNER')")
    public ResponseEntity<CompanyResponse> updateMyCompany(@Valid @RequestBody UpdateCompanyRequest request) {
        UUID companyId = SecurityUtil.getCompanyIdFromContext(); // Extrae el companyId del JWT/contexto
        return ResponseEntity.ok(companyService.updateCompany(companyId, request));
    }

    @SecurityRequirement(name = "bearerAuth")
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
