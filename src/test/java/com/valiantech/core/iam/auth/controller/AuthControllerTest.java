package com.valiantech.core.iam.auth.controller;

import com.valiantech.core.iam.auth.dto.*;
import com.valiantech.core.iam.auth.service.AuthService;
import com.valiantech.core.iam.auth.service.PasswordResetService;
import com.valiantech.core.iam.exception.UnauthorizedException;
import com.valiantech.core.iam.user.dto.UserResponse;
import com.valiantech.core.iam.user.model.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    AuthService authService;
    @Mock
    PasswordResetService passwordResetService;

    @InjectMocks
    AuthController controller;

    String email = "user@email.com";
    String password = "123456";
    String loginTicket = UUID.randomUUID().toString();
    UUID userId = UUID.randomUUID();
    UUID companyId = UUID.randomUUID();

    UserResponse userResponse = new UserResponse(userId, "User", email, true, null, null, null, null);
    CompanySummary companySummary = new CompanySummary(companyId, "Empresa S.A.", "ADMIN");
    AssociatedCompanies associatedCompanies = new AssociatedCompanies(userResponse, List.of(companySummary), loginTicket);
    LoginResponse loginResponse = new LoginResponse("jwt-authToken", UUID.randomUUID().toString(), userResponse, companyId, "ADMIN");

    @Nested
    @DisplayName("login (POST /api/v1/auth/login)")
    class LoginTests {

        @Test
        @DisplayName("Debe retornar 200 y companies en caso de credenciales válidas")
        void shouldReturnCompaniesOnValidLogin() {
            LoginRequest req = new LoginRequest(email, password);
            when(authService.fetchCompanies(req)).thenReturn(associatedCompanies);

            ResponseEntity<AssociatedCompanies> response = controller.login(req);

            assertEquals(200, response.getStatusCode().value());
            assertEquals(associatedCompanies, response.getBody());
            verify(authService, times(1)).fetchCompanies(req);
        }

        @Test
        @DisplayName("Debe propagar UnauthorizedException cuando las credenciales son incorrectas")
        void shouldThrowUnauthorizedOnInvalidCredentials() {
            LoginRequest req = new LoginRequest(email, "bad");
            when(authService.fetchCompanies(req)).thenThrow(new UnauthorizedException("Credenciales inválidas"));

            UnauthorizedException thrown = assertThrows(
                    UnauthorizedException.class,
                    () -> controller.login(req)
            );
            assertEquals("Credenciales inválidas", thrown.getMessage());
            verify(authService, times(1)).fetchCompanies(req);
        }
    }

    @Nested
    @DisplayName("loginWithCompany (POST /api/v1/auth/login-with-company)")
    class LoginWithCompanyTests {

        @Test
        @DisplayName("Debe retornar 200 y LoginResponse en caso de ticket y afiliación válidos")
        void shouldReturnJwtOnValidTicketAndAffiliation() {
            TokenRequest req = new TokenRequest(email, companyId, loginTicket);
            when(authService.loginWithCompany(req)).thenReturn(loginResponse);

            ResponseEntity<LoginResponse> response = controller.loginWithCompany(req);

            assertEquals(200, response.getStatusCode().value());
            assertEquals(loginResponse, response.getBody());
            verify(authService, times(1)).loginWithCompany(req);
        }

        @Test
        @DisplayName("Debe propagar UnauthorizedException cuando el ticket es inválido")
        void shouldThrowUnauthorizedOnInvalidTicket() {
            TokenRequest req = new TokenRequest(email, companyId, loginTicket);
            when(authService.loginWithCompany(req)).thenThrow(new UnauthorizedException("Ticket inválido"));

            UnauthorizedException thrown = assertThrows(
                    UnauthorizedException.class,
                    () -> controller.loginWithCompany(req)
            );
            assertEquals("Ticket inválido", thrown.getMessage());
            verify(authService, times(1)).loginWithCompany(req);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/whoami")
    class WhoamiTest {
        @Test
        @DisplayName("Debe retornar 200 y los datos del usuario autenticado si el contexto es válido")
        void shouldReturn200AndUserSessionIfContextValid() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            String email = "juan.perez@empresa.com";
            String fullName = "Juan Pérez";
            boolean emailValidated = true;
            String status = "ACTIVE";
            String companyName = "Empresa S.A.";
            String role = "ADMIN";

            Map<String, Object> details = Map.of(
                    "email", email,
                    "fullName", fullName,
                    "emailValidated", emailValidated,
                    "status", status,
                    "companyId", companyId.toString(),
                    "companyName", companyName,
                    "role", role
            );

            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(new User(userId, null, null, null, null, null, null, null, null, null, null));
            when(authentication.getDetails()).thenReturn(details);

            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);

            try (MockedStatic<SecurityContextHolder> mockContextHolder = mockStatic(SecurityContextHolder.class)) {
                mockContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                // Act
                ResponseEntity<WhoamiResponse> response = controller.whoami();

                // Assert
                assertEquals(200, response.getStatusCode().value());
                WhoamiResponse body = response.getBody();
                assertNotNull(body);
                assertEquals(userId, body.userId());
                assertEquals(fullName, body.fullName());
                assertEquals(email, body.email());
                assertEquals(emailValidated, body.emailValidated());
                assertEquals(status, body.status());
                assertEquals(companyId, body.companyId());
                assertEquals(companyName, body.companyName());
                assertEquals(role, body.role());
            }
        }

        @Test
        @DisplayName("Debe devolver valores por defecto si detalles faltan en el contexto")
        void shouldReturnDefaultsIfDetailsMissing() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            Map<String, Object> details = Map.of(
                    "companyId", companyId.toString(),
                    "role", "VIEWER"
                    // Falta fullName, companyName, status, etc.
            );

            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(new User(userId, null, null, null, null, null, null, null, null, null, null));
            when(authentication.getDetails()).thenReturn(details);

            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);

            try (MockedStatic<SecurityContextHolder> mockContextHolder = mockStatic(SecurityContextHolder.class)) {
                mockContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                // Act
                ResponseEntity<WhoamiResponse> response = controller.whoami();

                // Assert
                assertEquals(200, response.getStatusCode().value());
                WhoamiResponse body = response.getBody();
                assertNotNull(body);
                assertEquals(userId, body.userId());
                assertNull(body.fullName());
                assertNull(body.email());
                assertFalse(body.emailValidated());
                assertEquals("unknown", body.status());
                assertEquals(companyId, body.companyId());
                assertEquals("unknown", body.companyName());
                assertEquals("VIEWER", body.role());
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshTokenTests {

        @Test
        @DisplayName("Debe retornar 200 y nuevo JWT y refresh token si refresh token es válido")
        void shouldReturn200AndTokensIfValid() {
            String oldRefresh = "old-refresh-token";
            String newJwt = "new.jwt.token";
            String newRefresh = "new-refresh-token";
            RefreshTokenRequest req = new RefreshTokenRequest(oldRefresh);
            RefreshTokenResponse resp = new RefreshTokenResponse(newJwt, newRefresh);

            when(authService.refreshAuthToken(req)).thenReturn(resp);

            var result = controller.refreshToken(req);

            assertEquals(200, result.getStatusCode().value());
            assertEquals(resp, result.getBody());
            verify(authService).refreshAuthToken(req);
        }

        @Test
        @DisplayName("Debe lanzar UnauthorizedException si el refresh token es inválido o revocado")
        void shouldThrowUnauthorizedIfRefreshInvalidOrRevoked() {
            RefreshTokenRequest req = new RefreshTokenRequest("invalid-refresh");
            when(authService.refreshAuthToken(req)).thenThrow(new UnauthorizedException("Invalid refresh token"));

            UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> controller.refreshToken(req));
            assertTrue(ex.getMessage().contains("Invalid refresh token"));
            verify(authService).refreshAuthToken(req);
        }

        @Test
        @DisplayName("Debe lanzar UnauthorizedException si el usuario es inválido o no existe")
        void shouldThrowUnauthorizedIfUserInvalid() {
            RefreshTokenRequest req = new RefreshTokenRequest("refresh-token-user-invalid");
            when(authService.refreshAuthToken(req)).thenThrow(new UnauthorizedException("Invalid refresh token or user"));

            UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> controller.refreshToken(req));
            assertTrue(ex.getMessage().contains("Invalid refresh token or user"));
            verify(authService).refreshAuthToken(req);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class LogoutTest {
        @Test
        @DisplayName("Should return LogoutResponse wrapped in ResponseEntity when logout is called")
        void shouldReturnLogoutResponseOnLogout() {
            // Arrange
            LogoutRequest request = new LogoutRequest("valid-refresh-token");
            LogoutResponse expectedResponse = new LogoutResponse("Logout successful");

            when(authService.logout(any(LogoutRequest.class))).thenReturn(expectedResponse);

            // Act
            ResponseEntity<LogoutResponse> responseEntity = controller.logout(request);

            // Assert
            assertNotNull(responseEntity);
            assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
            assertEquals(expectedResponse, responseEntity.getBody());

            verify(authService, times(1)).logout(request);
        }

        @Test
        @DisplayName("Should propagate UnauthorizedException thrown by AuthService.logout")
        void shouldPropagateUnauthorizedException() {
            // Arrange
            LogoutRequest request = new LogoutRequest("invalid-refresh-token");

            when(authService.logout(any(LogoutRequest.class)))
                    .thenThrow(new UnauthorizedException("Invalid refresh token"));

            // Act & Assert
            UnauthorizedException thrown = assertThrows(UnauthorizedException.class, () -> {
                controller.logout(request);
            });

            assertEquals("Invalid refresh token", thrown.getMessage());
            verify(authService, times(1)).logout(request);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/change-password")
    class ChangePasswordTest {
        @Test
        @DisplayName("Should return ChangePasswordResponse wrapped in ResponseEntity when changePassword is called")
        void shouldReturnChangePasswordResponseOnChangePassword() {
            // Arrange
            User user = new User();
            user.setId(UUID.randomUUID());

            ChangePasswordRequest request = new ChangePasswordRequest("oldPass123", "newPass123");
            ChangePasswordResponse expectedResponse = new ChangePasswordResponse("Password changed successfully. New login required.");

            when(authService.changePassword(eq(user.getId()), any(ChangePasswordRequest.class)))
                    .thenReturn(expectedResponse);

            // Act
            ResponseEntity<ChangePasswordResponse> responseEntity = controller.changePassword(user, request);

            // Assert
            assertNotNull(responseEntity);
            assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
            assertEquals(expectedResponse, responseEntity.getBody());

            verify(authService, times(1)).changePassword(user.getId(), request);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/reset-password")
    class ResetPasswordEndpointTests {

        @Test
        @DisplayName("Debe retornar 200 y mensaje de éxito si el reset es exitoso")
        void shouldReturn200OnSuccess() {
            ResetPasswordRequest req = new ResetPasswordRequest("token123", "Nuev0P@ss");

            // No lanza excepción, por lo tanto es éxito
            doNothing().when(passwordResetService).resetPassword(req);

            var response = controller.resetPassword(req);

            assertEquals(200, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals("Password reset successfully", response.getBody().message());
            verify(passwordResetService).resetPassword(req);
        }

        @Test
        @DisplayName("Debe propagar UnauthorizedException si el reset falla por token inválido")
        void shouldPropagateUnauthorizedExceptionOnInvalidToken() {
            ResetPasswordRequest req = new ResetPasswordRequest("badtoken", "Nuev0P@ss");
            doThrow(new UnauthorizedException("Invalid password reset token"))
                    .when(passwordResetService).resetPassword(req);

            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                    () -> controller.resetPassword(req));
            assertTrue(ex.getMessage().contains("Invalid password reset token"));
            verify(passwordResetService).resetPassword(req);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/request-reset-password/{email}")
    class RequestResetPasswordEndpointTests {

        @Test
        @DisplayName("Debe retornar 200 siempre con mensaje genérico si se solicita reset")
        void shouldAlwaysReturn200OnRequestReset() {
            String email = "correo@demo.com";
            doNothing().when(passwordResetService).createPasswordResetRequest(email);

            var response = controller.requestResetPassword(email);

            assertEquals(200, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals("The email will be sent if the email exists.", response.getBody().message());
            verify(passwordResetService).createPasswordResetRequest(email);
        }
    }

}
