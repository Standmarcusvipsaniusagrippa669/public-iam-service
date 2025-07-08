package com.valiantech.core.iam.user.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.valiantech.core.iam.exception.ConflictException;
import com.valiantech.core.iam.exception.NotFoundException;
import com.valiantech.core.iam.exception.UnauthorizedException;
import com.valiantech.core.iam.user.dto.*;
import com.valiantech.core.iam.user.service.UserService;
import com.valiantech.core.iam.user.model.UserStatus;
import com.valiantech.core.iam.security.SecurityUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    UserService userService;

    @InjectMocks
    UserController controller;

    UUID companyId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();


    @Nested
    @DisplayName("PATCH /api/v1/users/{id}")
    class PatchUserTests {

        @Test
        @DisplayName("Debe retornar 200 y usuario actualizado si el update es exitoso")
        void shouldReturn200AndUserOnSuccess() {
            UpdateUserRequest req = new UpdateUserRequest("Nuevo Nombre", "nuevo@email.com", UserStatus.ACTIVE, false);
            UserResponse userResponse = new UserResponse(userId, "Nuevo Nombre", "nuevo@email.com", true, UserStatus.ACTIVE, null, null, null);

            try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCompanyIdFromContext).thenReturn(companyId);
                when(userService.updateUser(userId, companyId, req)).thenReturn(userResponse);

                var result = controller.patch(userId, req);

                assertEquals(200, result.getStatusCode().value());
                assertEquals(userResponse, result.getBody());
                verify(userService).updateUser(userId, companyId, req);
            }
        }

        @Test
        @DisplayName("Debe propagar NotFoundException si el usuario no existe")
        void shouldThrowNotFoundIfUserNotExists() {
            UpdateUserRequest req = new UpdateUserRequest("Otro Nombre", "otro@email.com", UserStatus.ACTIVE, false);

            try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCompanyIdFromContext).thenReturn(companyId);
                when(userService.updateUser(userId, companyId, req)).thenThrow(new NotFoundException("User not found"));

                NotFoundException ex = assertThrows(NotFoundException.class, () -> controller.patch(userId, req));
                assertTrue(ex.getMessage().contains("User not found"));
                verify(userService).updateUser(userId, companyId, req);
            }
        }

        @Test
        @DisplayName("Debe propagar ConflictException si el email ya está registrado")
        void shouldThrowConflictIfEmailAlreadyRegistered() {
            UpdateUserRequest req = new UpdateUserRequest("Otro Nombre", "otro@email.com", UserStatus.ACTIVE, false);

            try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCompanyIdFromContext).thenReturn(companyId);
                when(userService.updateUser(userId, companyId, req)).thenThrow(new ConflictException("Email already registered"));

                ConflictException ex = assertThrows(ConflictException.class, () -> controller.patch(userId, req));
                assertTrue(ex.getMessage().contains("Email already registered"));
                verify(userService).updateUser(userId, companyId, req);
            }
        }

        @Test
        @DisplayName("Debe propagar UnauthorizedException si no tiene permiso para actualizar")
        void shouldThrowUnauthorizedIfNoPermission() {
            UpdateUserRequest req = new UpdateUserRequest("Otro Nombre", "otro@email.com", UserStatus.ACTIVE, false);

            try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCompanyIdFromContext).thenReturn(companyId);
                when(userService.updateUser(userId, companyId, req)).thenThrow(new UnauthorizedException("Update not allowed"));

                UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> controller.patch(userId, req));
                assertTrue(ex.getMessage().contains("Update not allowed"));
                verify(userService).updateUser(userId, companyId, req);
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{id}")
    class GetUserTests {

        @Test
        @DisplayName("Debe retornar 200 y el usuario si existe y tiene acceso")
        void shouldReturn200AndUserIfExists() {
            UserResponse userResponse = new UserResponse(userId, "Usuario", "usuario@email.com", true, UserStatus.ACTIVE, null, null, null);

            try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCompanyIdFromContext).thenReturn(companyId);
                when(userService.getUserByCompanyId(userId, companyId)).thenReturn(userResponse);

                var result = controller.get(userId);

                assertEquals(200, result.getStatusCode().value());
                assertEquals(userResponse, result.getBody());
                verify(userService).getUserByCompanyId(userId, companyId);
            }
        }

        @Test
        @DisplayName("Debe propagar NotFoundException si el usuario no existe")
        void shouldThrowNotFoundIfUserNotExists() {
            try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCompanyIdFromContext).thenReturn(companyId);
                when(userService.getUserByCompanyId(userId, companyId)).thenThrow(new NotFoundException("User not found"));

                NotFoundException ex = assertThrows(NotFoundException.class, () -> controller.get(userId));
                assertTrue(ex.getMessage().contains("User not found"));
                verify(userService).getUserByCompanyId(userId, companyId);
            }
        }

        @Test
        @DisplayName("Debe propagar UnauthorizedException si no tiene permiso para obtener el usuario")
        void shouldThrowUnauthorizedIfNoPermission() {
            try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCompanyIdFromContext).thenReturn(companyId);
                when(userService.getUserByCompanyId(userId, companyId)).thenThrow(new UnauthorizedException("Get not allowed"));

                UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> controller.get(userId));
                assertTrue(ex.getMessage().contains("Get not allowed"));
                verify(userService).getUserByCompanyId(userId, companyId);
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users")
    class ListUsersTests {

        @Test
        @DisplayName("Debe retornar 200 y la lista de usuarios para la empresa")
        void shouldReturn200AndUserList() {
            List<UserResponse> users = List.of(
                    new UserResponse(UUID.randomUUID(), "Usuario1", "user1@email.com", true, UserStatus.ACTIVE, null, null, null),
                    new UserResponse(UUID.randomUUID(), "Usuario2", "user2@email.com", true, UserStatus.ACTIVE, null, null, null)
            );

            try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCompanyIdFromContext).thenReturn(companyId);
                when(userService.listAll(companyId)).thenReturn(users);

                var result = controller.list();

                assertEquals(200, result.getStatusCode().value());
                assertEquals(users, result.getBody());
                verify(userService).listAll(companyId);
            }
        }
    }
}
