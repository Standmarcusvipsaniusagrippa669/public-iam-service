package com.valiantech.core.iam.company.controller;

import com.valiantech.core.iam.company.dto.*;
import com.valiantech.core.iam.company.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    @PostMapping
    public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CreateCompanyRequest request) {
        return ResponseEntity.ok(companyService.createCompany(request));
    }

    @Operation(
            summary = "Update an existing company",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Company updated"),
                    @ApiResponse(responseCode = "404", description = "Company not found")
            }
    )
    @PutMapping("/{id}")
    public ResponseEntity<CompanyResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCompanyRequest request) {
        return ResponseEntity.ok(companyService.updateCompany(id, request));
    }

    @Operation(
            summary = "Get a company by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Company found"),
                    @ApiResponse(responseCode = "404", description = "Company not found")
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(companyService.getCompany(id));
    }

    @Operation(
            summary = "List all companies",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List retrieved")
            }
    )
    @GetMapping
    public ResponseEntity<List<CompanyResponse>> list() {
        return ResponseEntity.ok(companyService.listCompanies());
    }
}
