package com.valiantech.core.iam.invitation.service;

import com.valiantech.core.iam.company.dto.CompanyResponse;
import com.valiantech.core.iam.company.service.CompanyService;
import com.valiantech.core.iam.invitation.dto.*;
import com.valiantech.core.iam.invitation.model.InvitationStatus;
import com.valiantech.core.iam.invitation.model.UserInvitation;
import com.valiantech.core.iam.invitation.repository.UserInvitationRepository;
import com.valiantech.core.iam.exception.ConflictException;
import com.valiantech.core.iam.exception.NotFoundException;
import com.valiantech.core.iam.user.dto.CreateUserRequest;
import com.valiantech.core.iam.user.dto.UserResponse;
import com.valiantech.core.iam.user.service.UserService;
import com.valiantech.core.iam.usercompany.service.UserCompanyService;
import com.valiantech.core.iam.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final UserInvitationRepository invitationRepository;
    private final UserService userService;
    private final CompanyService companyService;
    private final UserCompanyService userCompanyService;

    public InvitationResponse create(CreateInvitationRequest request) {
        UserResponse user = userService.getUser(request.invitedBy());
        ValidationUtils.validateUserIsActive(user);

        CompanyResponse company = companyService.getCompany(request.companyId());
        ValidationUtils.validateCompanyIsActive(company);

        String token = UUID.randomUUID().toString();

        UserInvitation invitation = UserInvitation.builder()
                .id(UUID.randomUUID())
                .invitedEmail(request.invitedEmail())
                .companyId(request.companyId())
                .role(request.role())
                .invitedBy(request.invitedBy())
                .invitationToken(token)
                .status(InvitationStatus.PENDING)
                .registrationUrl("https://auth.valianspa.com/register?token="+token)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return map(invitationRepository.save(invitation));
    }

    public UserResponse acceptAndRegister(AcceptAndRegisterRequest request) {
        UserInvitation invitation = invitationRepository.findByInvitationToken(request.token())
                .orElseThrow(() -> new NotFoundException("Invitation not found."));

        if (!InvitationStatus.PENDING.equals(invitation.getStatus()) && !InvitationStatus.ACCEPTED.equals(invitation.getStatus())) {
            throw new ConflictException("Invitation not valid.");
        }
        if (invitation.getExpiresAt().isBefore(Instant.now())) {
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

        // Crea el vínculo usuario-empresa (user_companies)
        userCompanyService.registerUserCompanyByInvitation(
                userResponse.id(),
                invitation.getCompanyId(),
                invitation.getRole(),
                invitation.getInvitedBy()
        );

        // Marca la invitación como aceptada
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        invitation.setUpdatedAt(Instant.now());
        invitationRepository.save(invitation);

        return userResponse;
    }

    public InvitationResponse getByToken(String token) {
        UserInvitation invitation = invitationRepository.findByInvitationToken(token)
                .orElseThrow(() -> new NotFoundException("Invitation not found."));

        // Puedes validar aquí si el estado es válido para registro
        if (InvitationStatus.EXPIRED.equals(invitation.getStatus())) {
            throw new ConflictException("Invitation expired.");
        }

        return map(invitation);
    }

    public List<InvitationResponse> listAll() {
        return invitationRepository.findAll().stream().map(this::map).toList();
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
