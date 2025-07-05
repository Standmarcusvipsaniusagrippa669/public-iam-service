package com.valiantech.core.iam.usercompany.repository;

import com.valiantech.core.iam.usercompany.model.UserCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserCompanyRepository extends JpaRepository<UserCompany, UUID> {
    List<UserCompany> findByUserId(UUID userId);
    Optional<UserCompany> findByUserIdAndCompanyId(UUID userId, UUID companyId);
    @Query("SELECT u.userId FROM UserCompany u WHERE u.companyId = :companyId")
    List<UUID> findUserIdByCompanyId(UUID companyId);
}
