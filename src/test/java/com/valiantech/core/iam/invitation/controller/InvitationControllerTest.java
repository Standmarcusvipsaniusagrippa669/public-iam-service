package com.valiantech.core.iam.invitation.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.valiantech.core.iam.exception.ConflictException;
import com.valiantech.core.iam.exception.NotFoundException;
import com.valiantech.core.iam.invitation.controller.InvitationController;
import com.valiantech.core.iam.invitation.dto.*;
import com.valiantech.core.iam.invitation.model.InvitationStatus;
import com.valiantech.core.iam.invitation.service.InvitationService;
import com.valiantech.core.iam.security.SecurityUtil;
import com.valiantech.core.iam.user.dto.UserResponse;
import com.valiantech.core.iam.usercompany.model.UserCompanyRole;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationControllerTest {

    @Mock
    InvitationService invitationService;

    @InjectMocks
    InvitationController controller;

    UUID companyId = UUID.randomUUID();


    @Nested
    @DisplayName("POST /api/v1/invitations")
    class CreateInvitationTests {

        @Test
        @DisplayName("Debe retornar 200 y la invitación creada si es exitosa")
        void shouldReturn200AndInvitationOnSuccess() {
            UUID invitedBy = UUID.randomUUID();
            CreateInvitationRequest request = new CreateInvitationRequest(
                    UserCompanyRole.ADMIN,
                    "oneemail@domain.com",
                    invitedBy
            );
            InvitationResponse response = new InvitationResponse(
                    UUID.randomUUID(),
                    "email@domain.com",
                    companyId,
                    UserCompanyRole.ADMIN,
                    invitedBy,
                    UUID.randomUUID().toString(),
                    "",
                    InvitationStatus.PENDING,
                    Instant.now(),
                    Instant.now(),
                    Instant.now(),
                    Instant.now()
            );
            try (var mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCompanyIdFromContext).thenReturn(companyId);
                when(invitationService.create(companyId, request)).thenReturn(response);

                var result = controller.create(request);

                assertEquals(200, result.getStatusCode().value());
                assertEquals(response, result.getBody());
                verify(invitationService, times(1)).create(companyId, request);
            }

        }

        @Test
        @DisplayName("Debe propagar ConflictException si ya existe una invitación igual")
        void shouldThrowConflictIfInvitationAlreadyExists() {
            CreateInvitationRequest request = new CreateInvitationRequest(
                    UserCompanyRole.ADMIN,
                    "oneemail@domain.com",
                    UUID.randomUUID()
            );
            try (var mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCompanyIdFromContext).thenReturn(companyId);
                when(invitationService.create(companyId, request)).thenThrow(new ConflictException("Invitación duplicada"));

                ConflictException ex = assertThrows(ConflictException.class, () -> controller.create(request));
                assertTrue(ex.getMessage().contains("Invitación duplicada"));
                verify(invitationService, times(1)).create(companyId, request);
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/invitations/accept-and-register")
    class AcceptInvitationTests {

        @Test
        @DisplayName("Debe retornar 200 y el usuario creado si la invitación es aceptada")
        void shouldReturn200AndUserOnAccept() {
            AcceptAndRegisterRequest request = new AcceptAndRegisterRequest(
                    UUID.randomUUID().toString(),
                    "User",
                    "password"
            );
            UserResponse response = new UserResponse(UUID.randomUUID(), "User", "mail@e.cl", true, null, null, null, null);

            when(invitationService.acceptAndRegister(request)).thenReturn(response);

            var result = controller.accept(request);

            assertEquals(200, result.getStatusCode().value());
            assertEquals(response, result.getBody());
            verify(invitationService, times(1)).acceptAndRegister(request);
        }

        @Test
        @DisplayName("Debe propagar NotFoundException si el token de invitación no existe")
        void shouldThrowNotFoundIfTokenNotExists() {
            AcceptAndRegisterRequest request = new AcceptAndRegisterRequest(
                    UUID.randomUUID().toString(),
                    "User",
                    "password"
            );

            when(invitationService.acceptAndRegister(request)).thenThrow(new NotFoundException("No encontrada"));

            NotFoundException ex = assertThrows(NotFoundException.class, () -> controller.accept(request));
            assertTrue(ex.getMessage().contains("No encontrada"));
            verify(invitationService, times(1)).acceptAndRegister(request);
        }

        @Test
        @DisplayName("Debe propagar ConflictException si la invitación ya está aceptada o expirada")
        void shouldThrowConflictIfAlreadyAcceptedOrExpired() {
            AcceptAndRegisterRequest request = new AcceptAndRegisterRequest(
                    UUID.randomUUID().toString(),
                    "User",
                    "password"
            );

            when(invitationService.acceptAndRegister(request)).thenThrow(new ConflictException("Expirada"));

            ConflictException ex = assertThrows(ConflictException.class, () -> controller.accept(request));
            assertTrue(ex.getMessage().contains("Expirada"));
            verify(invitationService, times(1)).acceptAndRegister(request);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/invitations/{token}")
    class GetByTokenTests {

        @Test
        @DisplayName("Debe retornar 200 y los detalles de la invitación si existe el token")
        void shouldReturn200AndInvitationIfExists() {
            String token = "test-token";
            InvitationResponse response = new InvitationResponse(
                    UUID.randomUUID(),
                    "email@domain.com",
                    companyId,
                    UserCompanyRole.ADMIN,
                    UUID.randomUUID(),
                    UUID.randomUUID().toString(),
                    "",
                    InvitationStatus.PENDING,
                    Instant.now(),
                    Instant.now(),
                    Instant.now(),
                    Instant.now()
            );

            try (var mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCompanyIdFromContext).thenReturn(companyId);
                when(invitationService.getByToken(companyId, token)).thenReturn(response);

                var result = controller.getByToken(token);

                assertEquals(200, result.getStatusCode().value());
                assertEquals(response, result.getBody());
                verify(invitationService, times(1)).getByToken(companyId, token);
            }

        }

        @Test
        @DisplayName("Debe propagar NotFoundException si no existe la invitación")
        void shouldThrowNotFoundIfInvitationNotExists() {
            String token = "invalido";

            try (var mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCompanyIdFromContext).thenReturn(companyId);
                when(invitationService.getByToken(companyId, token)).thenThrow(new NotFoundException("No encontrada"));

                NotFoundException ex = assertThrows(NotFoundException.class, () -> controller.getByToken(token));
                assertTrue(ex.getMessage().contains("No encontrada"));
                verify(invitationService, times(1)).getByToken(companyId, token);
            }

        }
    }

    @Nested
    @DisplayName("GET /api/v1/invitations")
    class ListInvitationsTests {

        @Test
        @DisplayName("Debe retornar 200 y la lista de invitaciones")
        void shouldReturn200AndList() {
            List<InvitationResponse> responses = List.of(new InvitationResponse(
                    UUID.randomUUID(),
                    "email@domain.com",
                    companyId,
                    UserCompanyRole.ADMIN,
                    UUID.randomUUID(),
                    UUID.randomUUID().toString(),
                    "",
                    InvitationStatus.PENDING,
                    Instant.now(),
                    Instant.now(),
                    Instant.now(),
                    Instant.now()
            ), new InvitationResponse(
                    UUID.randomUUID(),
                    "email@domain.com",
                    companyId,
                    UserCompanyRole.ADMIN,
                    UUID.randomUUID(),
                    UUID.randomUUID().toString(),
                    "",
                    InvitationStatus.PENDING,
                    Instant.now(),
                    Instant.now(),
                    Instant.now(),
                    Instant.now()
            ));

            try (var mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCompanyIdFromContext).thenReturn(companyId);
                when(invitationService.listAll(companyId)).thenReturn(responses);

                var result = controller.list();

                assertEquals(200, result.getStatusCode().value());
                assertEquals(responses, result.getBody());
                verify(invitationService, times(1)).listAll(companyId);
            }

        }
    }
}
