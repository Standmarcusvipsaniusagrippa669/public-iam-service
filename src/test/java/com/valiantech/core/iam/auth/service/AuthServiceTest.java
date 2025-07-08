package com.valiantech.core.iam.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import com.valiantech.core.iam.auth.dto.*;
import com.valiantech.core.iam.auth.model.*;
import com.valiantech.core.iam.auth.repository.LoginTicketRepository;
import com.valiantech.core.iam.auth.repository.RefreshTokenRepository;
import com.valiantech.core.iam.company.model.*;
import com.valiantech.core.iam.company.repository.CompanyRepository;
import com.valiantech.core.iam.exception.UnauthorizedException;
import com.valiantech.core.iam.user.model.*;
import com.valiantech.core.iam.user.repository.UserRepository;
import com.valiantech.core.iam.usercompany.model.*;

import com.valiantech.core.iam.usercompany.repository.UserCompanyRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock
    UserCompanyRepository userCompanyRepository;
    @Mock
    CompanyRepository companyRepository;
    @Mock
    LoginTicketRepository loginTicketRepository;
    @Mock
    RefreshTokenRepository refreshTokenRepository;
    @InjectMocks AuthService authService;

    // Datos comunes
    final String email = "user@email.com";
    final String password = "pass123";
    final String encodedPassword = "encoded-pass123";
    final UUID userId = UUID.randomUUID();
    final UUID companyIdActive = UUID.randomUUID();
    final UUID companyIdInactive = UUID.randomUUID();
    final String loginTicket = UUID.randomUUID().toString();
    final Instant now = Instant.now();

    final User userActive = User.builder()
            .id(userId)
            .email(email)
            .passwordHash(encodedPassword)
            .status(UserStatus.ACTIVE)
            .build();

    final User userInactive = User.builder()
            .id(userId)
            .email(email)
            .passwordHash(encodedPassword)
            .status(UserStatus.DISABLED)
            .build();

    final Company companyActive = Company.builder()
            .id(companyIdActive)
            .businessName("Empresa Activa")
            .status(CompanyStatus.ACTIVE)
            .build();

    final Company companyInactive = Company.builder()
            .id(companyIdInactive)
            .businessName("Empresa Inactiva")
            .status(CompanyStatus.INACTIVE)
            .build();

    final UserCompany activeUserCompany = UserCompany.builder()
            .userId(userId)
            .companyId(companyIdActive)
            .status(UserCompanyStatus.ACTIVE)
            .role(UserCompanyRole.ADMIN)
            .build();

    final UserCompany inactiveUserCompany = UserCompany.builder()
            .userId(userId)
            .companyId(companyIdInactive)
            .status(UserCompanyStatus.DISABLED)
            .role(UserCompanyRole.VIEWER)
            .build();

    final LoginTicket ticket = LoginTicket.builder()
            .id(loginTicket)
            .email(email)
            .expiresAt(now.plusSeconds(300))
            .used(false)
            .build();

    @Nested
    @DisplayName("fetchCompanies")
    class FetchCompaniesTests {

        @ParameterizedTest(name = "{index}: {0}")
        @MethodSource("invalidLoginProvider")
        @DisplayName("Debe lanzar UnauthorizedException en credenciales inválidas o usuario inactivo")
        void shouldThrowUnauthorizedException(String caso, Optional<User> user, String inputPassword, boolean encoderMatches, UserStatus status) {
            // Arrange
            user.ifPresent(u -> u.setStatus(status));
            when(userRepository.findByEmail(email)).thenReturn(user);
            user.ifPresent(value -> when(passwordEncoder.matches(inputPassword, value.getPasswordHash())).thenReturn(encoderMatches));
            // Act & Assert
            assertThrows(UnauthorizedException.class, () ->
                    authService.fetchCompanies(new LoginRequest(email, inputPassword))
            );
        }

        static Stream<Arguments> invalidLoginProvider() {
            User userActive = User.builder().id(UUID.randomUUID()).email("x@x.com").passwordHash("abc").status(UserStatus.ACTIVE).build();
            User userInactive = User.builder().id(UUID.randomUUID()).email("x@x.com").passwordHash("abc").status(UserStatus.DISABLED).build();
            return Stream.of(
                    Arguments.of("Email no encontrado", Optional.empty(), "any", false, UserStatus.ACTIVE),
                    Arguments.of("Password incorrecto", Optional.of(userActive), "bad", false, UserStatus.ACTIVE),
                    Arguments.of("Usuario inactivo", Optional.of(userInactive), "abc", true, UserStatus.DISABLED)
            );
        }

        @Test
        @DisplayName("Debe devolver AssociatedCompanies solo con compañías activas y validar interacciones")
        void shouldReturnAssociatedCompaniesActivesAndVerifyCalled() {
            // Arrange
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(userActive));
            when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);
            when(loginTicketRepository.save(any(LoginTicket.class))).thenAnswer(i -> i.getArgument(0));

            when(userCompanyRepository.findByUserId(userId)).thenReturn(
                    Arrays.asList(activeUserCompany, inactiveUserCompany));
            when(companyRepository.findById(companyIdActive)).thenReturn(Optional.of(companyActive));

            LoginRequest request = new LoginRequest(email, password);

            // Act
            AssociatedCompanies resp = authService.fetchCompanies(request);

            // Assert interacciones y datos
            verify(userRepository, times(1)).findByEmail(email);
            verify(passwordEncoder, times(1)).matches(password, encodedPassword);
            verify(userCompanyRepository, times(1)).findByUserId(userId);
            verify(companyRepository, times(1)).findById(companyIdActive);
            verify(loginTicketRepository, times(1)).save(any(LoginTicket.class));

            assertEquals(email, resp.user().email());
            assertEquals(1, resp.companies().size());
            assertEquals(companyIdActive, resp.companies().get(0).companyId());
            assertEquals("Empresa Activa", resp.companies().get(0).companyName());
            assertEquals(activeUserCompany.getRole().name(), resp.companies().get(0).role());
            assertNotNull(resp.loginTicket());
        }
    }

    @Nested
    @DisplayName("loginWithCompany")
    class LoginWithCompanyTests {

        @ParameterizedTest(name = "{index}: {0}")
        @MethodSource("invalidTicketProvider")
        @DisplayName("Debe lanzar UnauthorizedException si ticket no existe, expiró, fue usado o email no coincide")
        void shouldThrowUnauthorizedByInvalidTicket(String caso, Optional<LoginTicket> ticketMock) {
            when(loginTicketRepository.findById(loginTicket)).thenReturn(ticketMock);
            TokenRequest req = new TokenRequest(email, companyIdActive, loginTicket);
            assertThrows(UnauthorizedException.class, () -> authService.loginWithCompany(req));
        }

        static Stream<Arguments> invalidTicketProvider() {
            return Stream.of(
                    Arguments.of("Ticket no existe", Optional.empty()),
                    Arguments.of("Ticket ya usado", Optional.of(LoginTicket.builder().id("t1").email("a@b.com").used(true).expiresAt(Instant.now().plusSeconds(300)).build())),
                    Arguments.of("Ticket expirado", Optional.of(LoginTicket.builder().id("t2").email("a@b.com").used(false).expiresAt(Instant.now().minusSeconds(10)).build())),
                    Arguments.of("Ticket email distinto", Optional.of(LoginTicket.builder().id("t3").email("other@email.com").used(false).expiresAt(Instant.now().plusSeconds(300)).build()))
            );
        }

        @Test
        @DisplayName("Debe lanzar UnauthorizedException si usuario, afiliación o status no es válido")
        void shouldThrowUnauthorizedExceptionByUserOrAffiliation() {
            when(loginTicketRepository.findById(loginTicket)).thenReturn(Optional.of(ticket));
            // Usuario no existe
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
            TokenRequest req = new TokenRequest(email, companyIdActive, loginTicket);
            assertThrows(UnauthorizedException.class, () -> authService.loginWithCompany(req));

            // Usuario existe, afiliación no existe
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(userActive));
            when(userCompanyRepository.findByUserIdAndCompanyId(userId, companyIdActive)).thenReturn(Optional.empty());
            assertThrows(UnauthorizedException.class, () -> authService.loginWithCompany(req));

            // Afiliación existe pero inactiva
            when(userCompanyRepository.findByUserIdAndCompanyId(userId, companyIdActive))
                    .thenReturn(Optional.of(inactiveUserCompany));
            assertThrows(UnauthorizedException.class, () -> authService.loginWithCompany(req));
        }

        @Test
        @DisplayName("Debe devolver LoginResponse, marcar ticket como usado y validar interacciones")
        void shouldReturnLoginResponseAndVerifyCalls() {
            // Arrange
            when(loginTicketRepository.findById(loginTicket)).thenReturn(Optional.of(ticket));
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(userActive));
            when(userCompanyRepository.findByUserIdAndCompanyId(userId, companyIdActive)).thenReturn(Optional.of(activeUserCompany));
            String jwt = "jwt-authToken";
            when(jwtService.generateToken(userActive, companyIdActive, activeUserCompany.getRole().name())).thenReturn(jwt);
            when(loginTicketRepository.save(any(LoginTicket.class))).thenReturn(ticket);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

            TokenRequest req = new TokenRequest(email, companyIdActive, loginTicket);

            // Act
            LoginResponse resp = authService.loginWithCompany(req);

            // Assert interacciones y datos
            verify(loginTicketRepository, times(1)).findById(loginTicket);
            verify(userRepository, times(1)).findByEmail(email);
            verify(jwtService, times(1)).generateToken(userActive, companyIdActive, activeUserCompany.getRole().name());
            verify(loginTicketRepository, times(1)).save(any(LoginTicket.class));

            assertEquals(jwt, resp.authToken());
            assertEquals(email, resp.user().email());
            assertEquals(companyIdActive, resp.companyId());
            assertEquals(activeUserCompany.getRole().name(), resp.role());
        }
    }
}
