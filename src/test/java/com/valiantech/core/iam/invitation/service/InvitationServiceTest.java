package com.valiantech.core.iam.invitation.service;

import static org.junit.jupiter.api.Assertions.*;

import com.valiantech.core.iam.audit.service.UserAuditLogService;
import com.valiantech.core.iam.company.dto.CompanyResponse;
import com.valiantech.core.iam.company.model.CompanyStatus;
import com.valiantech.core.iam.config.InvitationProperties;
import com.valiantech.core.iam.exception.ConflictException;
import com.valiantech.core.iam.exception.NotFoundException;
import com.valiantech.core.iam.invitation.dto.*;
import com.valiantech.core.iam.invitation.model.InvitationStatus;
import com.valiantech.core.iam.invitation.model.UserInvitation;
import com.valiantech.core.iam.invitation.repository.UserInvitationRepository;
import com.valiantech.core.iam.user.dto.UserResponse;
import com.valiantech.core.iam.user.model.UserStatus;
import com.valiantech.core.iam.usercompany.model.UserCompany;
import com.valiantech.core.iam.usercompany.model.UserCompanyRole;
import com.valiantech.core.iam.usercompany.service.UserCompanyService;
import com.valiantech.core.iam.user.service.UserService;
import com.valiantech.core.iam.company.service.CompanyService;
import com.valiantech.core.iam.util.ClientInfoService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock UserInvitationRepository invitationRepository;
    @Mock UserService userService;
    @Mock CompanyService companyService;
    @Mock UserCompanyService userCompanyService;
    @Mock
    InvitationProperties invitationProperties;
    @Mock
    UserAuditLogService userAuditLogService;
    @Mock
    ClientInfoService clientInfoService;
    @InjectMocks
    InvitationService invitationService;

    final UUID companyId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    final UUID invitedBy = UUID.randomUUID();
    final String invitedEmail = "invite@e.cl";
    final String token = "invitation-token-123";
    final Instant now = Instant.now();

    @Nested
    @DisplayName("create")
    class CreateInvitationTests {

        @Test
        @DisplayName("Debe crear la invitación si todo es válido")
        void shouldCreateInvitationIfValid() {
            CreateInvitationRequest req = new CreateInvitationRequest(UserCompanyRole.ADMIN, invitedEmail, invitedBy);
            UserResponse userResponse = new UserResponse(invitedBy, "Inviter", "inviter@e.cl", true, UserStatus.ACTIVE, null, null, null);
            CompanyResponse companyResponse = new CompanyResponse(companyId, "12.345.678-9", "Empresa", "Empresa", "Act", "Dir", "Com", "Reg", "c@e.cl", "123", "logo", CompanyStatus.ACTIVE, null, null);

            when(userService.getUser(invitedBy)).thenReturn(userResponse);
            when(companyService.getCompany(companyId)).thenReturn(companyResponse);
            UserCompany userCompany = UserCompany.builder().userId(invitedBy).companyId(companyId).role(UserCompanyRole.OWNER).build();
            when(userCompanyService.getUserCompany(invitedBy, companyId)).thenReturn(Optional.of(userCompany));
            // invitationProperties
            when(invitationProperties.getRegistrationUrlBase()).thenReturn("https://register.url/");
            when(invitationProperties.getTokenExpiryDays()).thenReturn(5);
            doNothing().when(userAuditLogService).logAsync(any());
            when(clientInfoService.getUserAgent()).thenReturn("Agent");
            when(clientInfoService.getClientIp()).thenReturn("0.0.0.0");
            when(clientInfoService.getCookies()).thenReturn("");
            UserInvitation savedInvitation = UserInvitation.builder()
                    .id(UUID.randomUUID())
                    .invitedEmail(invitedEmail)
                    .companyId(companyId)
                    .role(UserCompanyRole.ADMIN)
                    .invitedBy(invitedBy)
                    .invitationToken(token)
                    .status(InvitationStatus.PENDING)
                    .registrationUrl("https://register.url/" + token)
                    .expiresAt(now.plus(5, ChronoUnit.DAYS))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            when(invitationRepository.save(any(UserInvitation.class))).thenReturn(savedInvitation);

            InvitationResponse response = invitationService.create(companyId, req);

            assertEquals(invitedEmail, response.invitedEmail());
            assertEquals(companyId, response.companyId());
            assertEquals(UserCompanyRole.ADMIN, response.role());
            assertEquals(invitedBy, response.invitedBy());
            assertEquals("https://register.url/" + token, response.registrationUrl());
            assertEquals(InvitationStatus.PENDING, response.status());
            verify(invitationRepository).save(any(UserInvitation.class));
        }

        @Test
        @DisplayName("Debe lanzar ConflictException si el usuario no está asociado a la empresa")
        void shouldThrowConflictIfUserNotAssociated() {
            CreateInvitationRequest req = new CreateInvitationRequest(UserCompanyRole.ADMIN, invitedEmail, invitedBy);
            UserResponse userResponse = new UserResponse(invitedBy, "Inviter", "inviter@e.cl", true, UserStatus.ACTIVE, null, null, null);
            CompanyResponse companyResponse = new CompanyResponse(companyId, "12.345.678-9", "Empresa", "Empresa", "Act", "Dir", "Com", "Reg", "c@e.cl", "123", "logo", CompanyStatus.ACTIVE, null, null);

            when(userService.getUser(invitedBy)).thenReturn(userResponse);
            when(companyService.getCompany(companyId)).thenReturn(companyResponse);
            when(userCompanyService.getUserCompany(invitedBy, companyId)).thenReturn(Optional.empty());

            assertThrows(ConflictException.class, () -> invitationService.create(companyId, req));
        }
    }

    @Nested
    @DisplayName("acceptAndRegister")
    class AcceptAndRegisterTests {

        @Test
        @DisplayName("Debe aceptar y registrar usuario si la invitación está pendiente y no expirada")
        void shouldAcceptAndRegisterUserIfValid() {
            AcceptAndRegisterRequest req = new AcceptAndRegisterRequest(token, "Usuario Nuevo", "clave123");
            UserInvitation invitation = UserInvitation.builder()
                    .id(UUID.randomUUID())
                    .invitedEmail(invitedEmail)
                    .companyId(companyId)
                    .role(UserCompanyRole.ADMIN)
                    .invitedBy(invitedBy)
                    .invitationToken(token)
                    .status(InvitationStatus.PENDING)
                    .expiresAt(now.plus(1, ChronoUnit.DAYS))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            when(invitationRepository.findByInvitationToken(token)).thenReturn(Optional.of(invitation));
            UserResponse userResponse = new UserResponse(userId, "Usuario Nuevo", invitedEmail, true, null, null, null, null);
            when(userService.registerActiveUser(any())).thenReturn(userResponse);
            when(invitationRepository.save(any(UserInvitation.class))).thenReturn(invitation);
            doNothing().when(userAuditLogService).logAsync(any());
            when(clientInfoService.getUserAgent()).thenReturn("Agent");
            when(clientInfoService.getClientIp()).thenReturn("0.0.0.0");
            when(clientInfoService.getCookies()).thenReturn("");

            UserResponse response = invitationService.acceptAndRegister(req);

            assertEquals(userId, response.id());
            assertEquals(invitedEmail, response.email());
            verify(invitationRepository).save(invitation);
            verify(userCompanyService).registerUserCompanyByInvitation(userId, companyId, UserCompanyRole.ADMIN, invitedBy);
        }

        @Test
        @DisplayName("Debe lanzar NotFoundException si el token no existe")
        void shouldThrowNotFoundIfTokenNotExists() {
            AcceptAndRegisterRequest req = new AcceptAndRegisterRequest(token, "Usuario", "clave");
            when(invitationRepository.findByInvitationToken(token)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> invitationService.acceptAndRegister(req));
        }

        @Test
        @DisplayName("Debe lanzar ConflictException si la invitación no está pendiente o ya aceptada")
        void shouldThrowConflictIfInvitationNotPendingOrAccepted() {
            AcceptAndRegisterRequest req = new AcceptAndRegisterRequest(token, "Usuario", "clave");
            UserInvitation invitation = UserInvitation.builder()
                    .status(InvitationStatus.ACCEPTED)
                    .expiresAt(now.plus(1, ChronoUnit.DAYS))
                    .build();
            when(invitationRepository.findByInvitationToken(token)).thenReturn(Optional.of(invitation));

            assertThrows(ConflictException.class, () -> invitationService.acceptAndRegister(req));
        }

        @Test
        @DisplayName("Debe lanzar ConflictException si la invitación está expirada")
        void shouldThrowConflictIfInvitationExpired() {
            AcceptAndRegisterRequest req = new AcceptAndRegisterRequest(token, "Usuario", "clave");
            UserInvitation invitation = UserInvitation.builder()
                    .status(InvitationStatus.PENDING)
                    .expiresAt(now.minus(1, ChronoUnit.DAYS))
                    .build();
            when(invitationRepository.findByInvitationToken(token)).thenReturn(Optional.of(invitation));

            assertThrows(ConflictException.class, () -> invitationService.acceptAndRegister(req));
        }
    }

    @Nested
    @DisplayName("getByToken")
    class GetByTokenTests {

        @Test
        @DisplayName("Debe retornar los datos de la invitación si el token es válido")
        void shouldReturnInvitationIfTokenValid() {
            UserInvitation invitation = UserInvitation.builder()
                    .id(UUID.randomUUID())
                    .invitedEmail(invitedEmail)
                    .companyId(companyId)
                    .role(UserCompanyRole.ADMIN)
                    .invitedBy(invitedBy)
                    .invitationToken(token)
                    .status(InvitationStatus.PENDING)
                    .expiresAt(now.plus(1, ChronoUnit.DAYS))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            when(invitationRepository.findByInvitationTokenAndCompanyId(token, companyId)).thenReturn(Optional.of(invitation));

            InvitationResponse response = invitationService.getByToken(companyId, token);

            assertEquals(invitedEmail, response.invitedEmail());
            assertEquals(token, response.invitationToken());
        }

        @Test
        @DisplayName("Debe lanzar NotFoundException si el token no existe")
        void shouldThrowNotFoundIfTokenNotExists() {
            when(invitationRepository.findByInvitationTokenAndCompanyId(token, companyId)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> invitationService.getByToken(companyId, token));
        }

        @Test
        @DisplayName("Debe lanzar ConflictException si la invitación está expirada")
        void shouldThrowConflictIfInvitationExpired() {
            UserInvitation invitation = UserInvitation.builder()
                    .status(InvitationStatus.EXPIRED)
                    .expiresAt(now.minus(1, ChronoUnit.DAYS))
                    .build();
            when(invitationRepository.findByInvitationTokenAndCompanyId(token, companyId)).thenReturn(Optional.of(invitation));

            assertThrows(ConflictException.class, () -> invitationService.getByToken(companyId, token));
        }
    }

    @Nested
    @DisplayName("listAll")
    class ListAllTests {
        @Test
        @DisplayName("Debe retornar la lista de invitaciones")
        void shouldReturnListOfInvitations() {
            List<UserInvitation> invitations = List.of(
                    UserInvitation.builder().id(UUID.randomUUID()).invitedEmail("a@e.cl").build(),
                    UserInvitation.builder().id(UUID.randomUUID()).invitedEmail("b@e.cl").build()
            );
            when(invitationRepository.findAllByCompanyId(companyId)).thenReturn(invitations);

            List<InvitationResponse> responses = invitationService.listAll(companyId);

            assertEquals(2, responses.size());
            assertEquals("a@e.cl", responses.get(0).invitedEmail());
            assertEquals("b@e.cl", responses.get(1).invitedEmail());
        }
    }
}
