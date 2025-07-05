package com.valiantech.core.iam.usercompany.service;

import com.valiantech.core.iam.usercompany.model.UserCompany;
import com.valiantech.core.iam.usercompany.model.UserCompanyRole;
import com.valiantech.core.iam.usercompany.model.UserCompanyStatus;
import com.valiantech.core.iam.usercompany.repository.UserCompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserCompanyService {

    private final UserCompanyRepository userCompanyRepository;

    public UserCompany registerUserCompanyByInvitation(UUID userId, UUID companyId, UserCompanyRole role, UUID invitedBy) {
        // Puedes validar unicidad aquí si quieres
        if (userCompanyRepository.findByUserIdAndCompanyId(userId, companyId).isPresent()) {
            throw new RuntimeException("This user is already linked to the company.");
        }

        UserCompany uc = UserCompany.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .companyId(companyId)
                .role(role)
                .invitedBy(invitedBy)
                .status(UserCompanyStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return userCompanyRepository.save(uc);
    }

    public UserCompany registerOwnerCompany(UUID userId, UUID companyId, UserCompanyRole role) {
        UserCompany uc = UserCompany.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .companyId(companyId)
                .role(role)
                .invitedBy(null)
                .status(UserCompanyStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return userCompanyRepository.save(uc);
    }

    public Optional<UserCompany> getUserCompany(UUID userId, UUID companyId) {
        return userCompanyRepository.findByUserIdAndCompanyId(userId, companyId);
    }
}
