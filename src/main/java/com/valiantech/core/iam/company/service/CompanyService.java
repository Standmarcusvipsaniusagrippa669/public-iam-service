package com.valiantech.core.iam.company.service;

import com.valiantech.core.iam.company.dto.*;
import com.valiantech.core.iam.company.model.Company;
import com.valiantech.core.iam.company.repository.CompanyRepository;
import com.valiantech.core.iam.exception.ConflictException;
import com.valiantech.core.iam.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyResponse createCompany(CreateCompanyRequest request) {
        if (companyRepository.findByRut(request.rut()).isPresent()) {
            throw new ConflictException("Company with this RUT already exists.");
        }

        Company entity = Company.builder()
                .id(UUID.randomUUID())
                .rut(request.rut())
                .businessName(request.businessName())
                .tradeName(request.tradeName())
                .activity(request.activity())
                .address(request.address())
                .commune(request.commune())
                .region(request.region())
                .email(request.email())
                .phone(request.phone())
                .logoUrl(request.logoUrl())
                .status("active")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return map(companyRepository.save(entity));
    }

    public CompanyResponse updateCompany(UUID id, UpdateCompanyRequest request) {
        Company entity = companyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Company not found."));

        if (request.businessName() != null) entity.setBusinessName(request.businessName());
        if (request.tradeName() != null) entity.setTradeName(request.tradeName());
        if (request.activity() != null) entity.setActivity(request.activity());
        if (request.address() != null) entity.setAddress(request.address());
        if (request.commune() != null) entity.setCommune(request.commune());
        if (request.region() != null) entity.setRegion(request.region());
        if (request.email() != null) entity.setEmail(request.email());
        if (request.phone() != null) entity.setPhone(request.phone());
        if (request.logoUrl() != null) entity.setLogoUrl(request.logoUrl());
        if (request.status() != null) entity.setStatus(request.status());

        entity.setUpdatedAt(Instant.now());

        return map(companyRepository.save(entity));
    }

    public CompanyResponse getCompany(UUID id) {
        Company entity = companyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Company not found."));
        return map(entity);
    }

    public List<CompanyResponse> listCompanies() {
        return companyRepository.findAll().stream().map(this::map).toList();
    }

    private CompanyResponse map(Company c) {
        return new CompanyResponse(
                c.getId(),
                c.getRut(),
                c.getBusinessName(),
                c.getTradeName(),
                c.getActivity(),
                c.getAddress(),
                c.getCommune(),
                c.getRegion(),
                c.getEmail(),
                c.getPhone(),
                c.getLogoUrl(),
                c.getStatus(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
