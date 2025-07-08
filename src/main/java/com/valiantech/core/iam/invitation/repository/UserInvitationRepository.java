package com.valiantech.core.iam.invitation.repository;

import com.valiantech.core.iam.invitation.model.UserInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserInvitationRepository extends JpaRepository<UserInvitation, UUID> {
    Optional<UserInvitation> findByInvitationToken(String token);
    Optional<UserInvitation> findByInvitationTokenAndCompanyId(String token, UUID companyId);
    List<UserInvitation> findAllByCompanyId(UUID companyId);
}
