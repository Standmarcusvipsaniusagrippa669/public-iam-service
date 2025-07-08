package com.valiantech.core.iam.auth.controller;

import com.valiantech.core.iam.auth.dto.*;
import com.valiantech.core.iam.auth.service.AuthService;
import com.valiantech.core.iam.exception.UnauthorizedException;
import com.valiantech.core.iam.user.dto.UserResponse;
import com.valiantech.core.iam.user.model.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
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
    LoginResponse loginResponse = new LoginResponse("jwt-token", userResponse, companyId, "ADMIN");

    @Nested
    @DisplayName("login (POST /api/v1/auth/login)")
    class LoginTests {

        @Test
        @DisplayName("Debe retornar 200 y companies en caso de credenciales válidas")
        void shouldReturnCompaniesOnValidLogin() {
            LoginRequest req = new LoginRequest(email, password);
            when(authService.fetchCompanies(req)).thenReturn(associatedCompanies);

            ResponseEntity<AssociatedCompanies> response = controller.login(req);

            assertEquals(200, response.getStatusCodeValue());
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

            assertEquals(200, response.getStatusCodeValue());
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
                assertEquals(200, response.getStatusCodeValue());
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

}
