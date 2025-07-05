package com.valiantech.core.iam.company.service;

import com.valiantech.core.iam.company.dto.*;
import com.valiantech.core.iam.company.model.Company;
import com.valiantech.core.iam.company.model.CompanyStatus;
import com.valiantech.core.iam.company.repository.CompanyRepository;
import com.valiantech.core.iam.exception.ConflictException;
import com.valiantech.core.iam.exception.NotFoundException;
import com.valiantech.core.iam.user.dto.UserResponse;
import com.valiantech.core.iam.user.service.UserService;
import com.valiantech.core.iam.usercompany.model.UserCompanyRole;
import com.valiantech.core.iam.usercompany.service.UserCompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserService userService;
    private final UserCompanyService userCompanyService;

    @Transactional
    public CompanyResponse onboarding(CompanyOnboardingRequest request) {
        if (companyRepository.findByRut(request.company().rut()).isPresent()) {
            throw new ConflictException("Company with this RUT already exists.");
        }

        Company company = Company.builder()
                .id(UUID.randomUUID())
                .rut(request.company().rut())
                .businessName(request.company().businessName())
                .tradeName(request.company().tradeName())
                .activity(request.company().activity())
                .address(request.company().address())
                .commune(request.company().commune())
                .region(request.company().region())
                .email(request.company().email())
                .phone(request.company().phone())
                .logoUrl(request.company().logoUrl())
                .status(CompanyStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        company = companyRepository.save(company);


        // 2. Crear usuario fundador (activo y validado)
        UserResponse userResponse = userService.registerActiveUser(request.owner());
        // 3. Crear vínculo OWNER en user_companies
        userCompanyService.registerOwnerCompany(
                userResponse.id(),
                company.getId(),
                UserCompanyRole.OWNER
        );

        return map(company);
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
