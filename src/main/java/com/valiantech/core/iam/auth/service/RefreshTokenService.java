package com.valiantech.core.iam.auth.service;

import com.valiantech.core.iam.auth.model.RefreshToken;
import com.valiantech.core.iam.auth.repository.RefreshTokenRepository;
import com.valiantech.core.iam.exception.UnauthorizedException;
import com.valiantech.core.iam.security.SecurityUtil;
import com.valiantech.core.iam.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Guarda un nuevo refresh token en la base de datos.
     * <p>
     * Calcula el hash SHA-256 del token plano para almacenamiento seguro,
     * asigna metadatos como fechas de emisión y expiración, y marca el token como no revocado.
     * </p>
     *
     * @param companyId         el UUID de la empresa asociada al token.
     * @param user              el usuario asociado al token.
     */
    String saveNewRefreshToken(UUID companyId, User user) {
        String refreshTokenPlain = UUID.randomUUID().toString();
        // Calcula el hash SHA-256 del refresh token para almacenarlo seguro
        String refreshTokenHash = SecurityUtil.sha256Hex(refreshTokenPlain);

        // Crea la entidad RefreshToken para persistirla
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setCompanyId(companyId);
        refreshToken.setTokenHash(refreshTokenHash);
        refreshToken.setIssuedAt(Instant.now());
        refreshToken.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS)); // Expira en 30 días (ajustable)
        refreshToken.setRevoked(false);

        // Guarda el refresh token en la base de datos
        refreshTokenRepository.save(refreshToken);
        return refreshTokenPlain;
    }

    void updateRefreshToken(RefreshToken refreshToken) {
        refreshTokenRepository.save(refreshToken);
    }

    void revokeAllByUserId(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    RefreshToken findByTokenHash(String refreshTokenHash) {
        return refreshTokenRepository.findByTokenHash(refreshTokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
    }
}
