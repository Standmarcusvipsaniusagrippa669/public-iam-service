package com.valiantech.core.iam.usercompany.service;

import com.valiantech.core.iam.usercompany.model.UserCompany;
import com.valiantech.core.iam.usercompany.repository.UserCompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserCompanyService {

    private final UserCompanyRepository userCompanyRepository;

    public UserCompany createLink(UUID userId, UUID companyId, String role, UUID invitedBy) {
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
                .status("active")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return userCompanyRepository.save(uc);
    }
}
