package com.valiantech.core.iam.invitation.service;

import com.valiantech.core.iam.company.dto.CompanyResponse;
import com.valiantech.core.iam.company.service.CompanyService;
import com.valiantech.core.iam.config.InvitationProperties;
import com.valiantech.core.iam.invitation.dto.*;
import com.valiantech.core.iam.invitation.model.InvitationStatus;
import com.valiantech.core.iam.invitation.model.UserInvitation;
import com.valiantech.core.iam.invitation.repository.UserInvitationRepository;
import com.valiantech.core.iam.exception.ConflictException;
import com.valiantech.core.iam.exception.NotFoundException;
import com.valiantech.core.iam.user.dto.CreateUserRequest;
import com.valiantech.core.iam.user.dto.UserResponse;
import com.valiantech.core.iam.user.service.UserService;
import com.valiantech.core.iam.usercompany.model.UserCompany;
import com.valiantech.core.iam.usercompany.model.UserCompanyRole;
import com.valiantech.core.iam.usercompany.service.UserCompanyService;
import com.valiantech.core.iam.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class InvitationService {

    private final UserInvitationRepository invitationRepository;
    private final UserService userService;
    private final CompanyService companyService;
    private final UserCompanyService userCompanyService;

    private final InvitationProperties invitationProperties;

    public InvitationResponse create(UUID companyId, CreateInvitationRequest request) {
        log.debug("Starting create invitation for invitedEmail={}", request.invitedEmail());
        UserResponse user = userService.getUser(request.invitedBy());
        ValidationUtils.validateUserIsActive(user);
        log.debug("User manager found and is active for invitedEmail={}", request.invitedEmail());

        CompanyResponse company = companyService.getCompany(companyId);
        ValidationUtils.validateCompanyIsActive(company);
        log.debug("Company associated founded and is active for invitedEmail={}", request.invitedEmail());

        Optional<UserCompany> ucOpt = userCompanyService.getUserCompany(request.invitedBy(), companyId);
        if (ucOpt.isEmpty()) {
            log.warn("User is not associated with the company for invitedEmail={}", request.invitedEmail());
            throw new ConflictException("User is not associated with this company");
        }
        ValidationUtils.validateUserHasRole(ucOpt.get(), UserCompanyRole.OWNER, UserCompanyRole.ADMIN);
        log.debug("User manager has role Owner or admin for invitedEmail={}", request.invitedEmail());

        String token = UUID.randomUUID().toString();

        UserInvitation invitation = UserInvitation.builder()
                .id(UUID.randomUUID())
                .invitedEmail(request.invitedEmail())
                .companyId(companyId)
                .role(request.role())
                .invitedBy(request.invitedBy())
                .invitationToken(token)
                .status(InvitationStatus.PENDING)
                .registrationUrl(invitationProperties.getRegistrationUrlBase().concat(token))
                .expiresAt(Instant.now().plus(invitationProperties.getTokenExpiryDays(), ChronoUnit.DAYS))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        invitation = invitationRepository.save(invitation);
        log.info("Invitation create successfully for email {}", request.invitedEmail());
        return map(invitation);
    }

    public UserResponse acceptAndRegister(AcceptAndRegisterRequest request) {
        log.debug("Starting acceptAndRegister for token={}", request.token());
        UserInvitation invitation = invitationRepository.findByInvitationToken(request.token())
                .orElseThrow(() -> new NotFoundException("Invitation not found."));
        log.debug("Invitation found for token={}", request.token());
        if (!InvitationStatus.PENDING.equals(invitation.getStatus())) {
            log.warn("Invitation status is distinct to PENDING for token={}", request.token());
            throw new ConflictException("Invitation not valid.");
        }
        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Invitation expired for token={}", request.token());
            throw new ConflictException("Invitation expired.");
        }

        // Usa UserService para crear el usuario
        UserResponse userResponse = userService.registerActiveUser(
                new CreateUserRequest(
                        request.fullName(),
                        invitation.getInvitedEmail(),
                        request.password()
                )
        );
        log.debug("User invited register successfully for token={}", request.token());

        // Crea el vínculo usuario-empresa (user_companies)
        userCompanyService.registerUserCompanyByInvitation(
                userResponse.id(),
                invitation.getCompanyId(),
                invitation.getRole(),
                invitation.getInvitedBy()
        );
        log.debug("User invited associated to company for token={}", request.token());


        // Marca la invitación como aceptada
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        invitation.setUpdatedAt(Instant.now());
        invitationRepository.save(invitation);
        log.info("Invitation was accepted successfully with token {}", request.token());
        return userResponse;
    }

    public InvitationResponse getByToken(UUID companyId, String token) {
        UserInvitation invitation = invitationRepository.findByInvitationTokenAndCompanyId(token, companyId)
                .orElseThrow(() -> new NotFoundException("Invitation not found."));

        // Puedes validar aquí si el estado es válido para registro
        if (InvitationStatus.EXPIRED.equals(invitation.getStatus())) {
            throw new ConflictException("Invitation expired.");
        }

        return map(invitation);
    }

    public List<InvitationResponse> listAll(UUID companyId) {
        return invitationRepository.findAllByCompanyId(companyId).stream().map(this::map).toList();
    }

    private InvitationResponse map(UserInvitation i) {
        return new InvitationResponse(
                i.getId(),
                i.getInvitedEmail(),
                i.getCompanyId(),
                i.getRole(),
                i.getInvitedBy(),
                i.getInvitationToken(),
                i.getRegistrationUrl(),
                i.getStatus(),
                i.getExpiresAt(),
                i.getAcceptedAt(),
                i.getCreatedAt(),
                i.getUpdatedAt()
        );
    }
}
