package com.valiantech.core.iam.auth.service;

import com.valiantech.core.iam.auth.dto.ResetPasswordRequest;
import com.valiantech.core.iam.auth.model.ResetPasswordStatus;
import com.valiantech.core.iam.auth.model.UserPasswordReset;
import com.valiantech.core.iam.auth.repository.UserPasswordResetRepository;
import com.valiantech.core.iam.email.EmailSender;
import com.valiantech.core.iam.exception.UnauthorizedException;
import com.valiantech.core.iam.user.model.User;
import com.valiantech.core.iam.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock UserPasswordResetRepository passwordResetRepository;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock RefreshTokenService refreshTokenService;
    @Mock
    EmailSender emailSender;

    @InjectMocks PasswordResetService passwordResetService;

    final String email = "user@demo.com";
    final String newPassword = "Nuev0P@ssword";
    final UUID userId = UUID.randomUUID();
    final String resetToken = UUID.randomUUID().toString();

    @Nested
    @DisplayName("createPasswordResetRequest")
    class CreatePasswordResetRequestTests {

        @Test
        @DisplayName("Debe crear y guardar un reset token si el usuario existe")
        void shouldCreateResetRequestIfUserExists() {
            User user = User.builder().id(userId).email(email).build();
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            doNothing().when(emailSender).sendEmail(anyString(), anyString(), anyString());
            passwordResetService.createPasswordResetRequest(email);

            verify(passwordResetRepository).save(argThat(reset ->
                    reset.getUserId().equals(userId)
                            && reset.getStatus().equals(ResetPasswordStatus.REQUESTED)
                            && reset.getExpiresAt().isAfter(Instant.now())
            ));
        }

        @Test
        @DisplayName("No debe hacer nada si el usuario no existe")
        void shouldDoNothingIfUserNotFound() {
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            passwordResetService.createPasswordResetRequest(email);

            verify(passwordResetRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPasswordTests {

        @Test
        @DisplayName("Debe permitir el reset si token válido y usuario existe")
        void shouldResetPasswordIfTokenValidAndUserExists() {
            ResetPasswordRequest req = new ResetPasswordRequest(resetToken, newPassword);
            UserPasswordReset upr = UserPasswordReset.builder()
                    .userId(userId)
                    .resetToken(resetToken)
                    .status(ResetPasswordStatus.REQUESTED)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
            User user = User.builder().id(userId).build();

            when(passwordResetRepository.findByResetToken(resetToken)).thenReturn(Optional.of(upr));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(newPassword)).thenReturn("hashed");

            passwordResetService.resetPassword(req);

            assertEquals(ResetPasswordStatus.USED, upr.getStatus());
            assertNotNull(upr.getUsedAt());
            verify(userRepository).save(user);
            verify(passwordResetRepository).save(upr);
            verify(refreshTokenService).revokeAllByUserId(userId);
        }

        @Test
        @DisplayName("Debe lanzar UnauthorizedException si el token no existe")
        void shouldThrowIfTokenNotFound() {
            ResetPasswordRequest req = new ResetPasswordRequest(resetToken, newPassword);
            when(passwordResetRepository.findByResetToken(resetToken)).thenReturn(Optional.empty());

            assertThrows(UnauthorizedException.class, () -> passwordResetService.resetPassword(req));
        }

        @Test
        @DisplayName("Debe lanzar UnauthorizedException si el token ya fue usado o revocado")
        void shouldThrowIfTokenAlreadyUsedOrRevoked() {
            ResetPasswordRequest req = new ResetPasswordRequest(resetToken, newPassword);
            UserPasswordReset upr = UserPasswordReset.builder()
                    .userId(userId)
                    .resetToken(resetToken)
                    .status(ResetPasswordStatus.USED)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
            when(passwordResetRepository.findByResetToken(resetToken)).thenReturn(Optional.of(upr));

            assertThrows(UnauthorizedException.class, () -> passwordResetService.resetPassword(req));
        }

        @Test
        @DisplayName("Debe lanzar UnauthorizedException si el token expiró")
        void shouldThrowIfTokenExpired() {
            ResetPasswordRequest req = new ResetPasswordRequest(resetToken, newPassword);
            UserPasswordReset upr = UserPasswordReset.builder()
                    .userId(userId)
                    .resetToken(resetToken)
                    .status(ResetPasswordStatus.REQUESTED)
                    .expiresAt(Instant.now().minusSeconds(60))
                    .build();
            when(passwordResetRepository.findByResetToken(resetToken)).thenReturn(Optional.of(upr));

            assertThrows(UnauthorizedException.class, () -> passwordResetService.resetPassword(req));
        }

        @Test
        @DisplayName("Debe lanzar UnauthorizedException si el usuario asociado al reset no existe")
        void shouldThrowIfUserNotFound() {
            ResetPasswordRequest req = new ResetPasswordRequest(resetToken, newPassword);
            UserPasswordReset upr = UserPasswordReset.builder()
                    .userId(userId)
                    .resetToken(resetToken)
                    .status(ResetPasswordStatus.REQUESTED)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            when(passwordResetRepository.findByResetToken(resetToken)).thenReturn(Optional.of(upr));
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(UnauthorizedException.class, () -> passwordResetService.resetPassword(req));
        }
    }
}
