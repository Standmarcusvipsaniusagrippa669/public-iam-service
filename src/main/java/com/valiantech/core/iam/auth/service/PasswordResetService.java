package com.valiantech.core.iam.auth.service;

import com.valiantech.core.iam.auth.dto.ResetPasswordRequest;
import com.valiantech.core.iam.auth.model.ResetPasswordStatus;
import com.valiantech.core.iam.auth.model.UserPasswordReset;
import com.valiantech.core.iam.auth.repository.UserPasswordResetRepository;
import com.valiantech.core.iam.exception.UnauthorizedException;
import com.valiantech.core.iam.user.model.User;
import com.valiantech.core.iam.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio encargado de gestionar el flujo de recuperación y restablecimiento de contraseñas mediante token seguro.
 * <ul>
 *   <li>Permite solicitar un token temporal de recuperación de contraseña.</li>
 *   <li>Valida el token recibido, permite el cambio de clave y marca el token como usado.</li>
 *   <li>Revoca todos los refresh tokens activos del usuario tras un reset exitoso por seguridad.</li>
 *   <li>Tokens expirados, ya usados o inválidos provocan excepción {@link UnauthorizedException}.</li>
 * </ul>
 * <b>Notas:</b>
 * <ul>
 *   <li>El token expira por defecto en 2 horas.</li>
 *   <li>No se informa si el usuario existe al solicitar reset (medida de privacidad).</li>
 * </ul>
 * @author Ian Cardenas
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserPasswordResetRepository passwordResetRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    private static final int EXPIRATION_HOURS = 2;

    /**
     * Crea un nuevo request de reset password para el usuario indicado.
     *
     * @param email email del usuario que solicita reset
     * @return token generado para reset password
     */
    @Transactional
    public void createPasswordResetRequest(String email) {
        String token = UUID.randomUUID().toString();
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty())
            return;

        UserPasswordReset reset = UserPasswordReset.builder()
                .id(UUID.randomUUID())
                .userId(user.get().getId())
                .resetToken(token)
                .status(ResetPasswordStatus.REQUESTED)
                .requestedAt(Instant.now())
                .expiresAt(Instant.now().plus(EXPIRATION_HOURS, ChronoUnit.HOURS))
                .build();

        passwordResetRepository.save(reset);
    }

    /**
     * Valida el token de reset y cambia la contraseña del usuario asociado.
     *
     * @param ResetPasswordRequest Request cnn información para el reset del password
     * @throws UnauthorizedException si token inválido, expirado o ya usado
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        UserPasswordReset reset = passwordResetRepository.findByResetToken(request.resetToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid password reset token"));

        if (!ResetPasswordStatus.REQUESTED.equals(reset.getStatus())) {
            throw new UnauthorizedException("Password reset token already used or revoked");
        }

        if (reset.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Password reset token expired");
        }

        User user = userRepository.findById(reset.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not found for password reset"));

        // Cambiar contraseña
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Actualizar estado token
        reset.setStatus(ResetPasswordStatus.USED);
        reset.setUsedAt(Instant.now());
        passwordResetRepository.save(reset);

        refreshTokenService.revokeAllByUserId(user.getId());
    }
}
