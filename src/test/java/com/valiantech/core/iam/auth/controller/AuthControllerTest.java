package com.valiantech.core.iam.auth.controller;

import com.valiantech.core.iam.auth.dto.*;
import com.valiantech.core.iam.auth.service.AuthService;
import com.valiantech.core.iam.exception.UnauthorizedException;
import com.valiantech.core.iam.user.dto.UserResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
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
}
