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
/**
 * Servicio encargado de la gestión segura de refresh tokens para autenticación.
 * <p>
 * - Genera, almacena y revoca refresh tokens asociados a usuarios y compañías.<br>
 * - Los refresh tokens se almacenan utilizando un hash SHA-256, nunca el valor plano.<br>
 * - Permite revocación masiva por usuario, extensión de expiración y búsqueda validada por hash.<br>
 * </p>
 * <b>Notas de seguridad:</b>
 * <ul>
 *   <li>El token plano solo se retorna una vez al crear; después, solo se opera sobre el hash.</li>
 *   <li>Un refresh token puede ser revocado en cualquier momento (logout o seguridad).</li>
 *   <li>La expiración es configurable (por defecto, 30 días).</li>
 * </ul>
 * @author Ian Cardenas
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Crea y almacena un nuevo refresh token (hashed) para el usuario y la empresa indicados.
     * <p>
     * - Genera un UUID como token plano.<br>
     * - Calcula el hash SHA-256 del token para almacenamiento seguro.<br>
     * - Persiste el hash, usuario, empresa, fechas y estado en la base de datos.<br>
     * - Retorna solo el token plano para entregar al cliente.<br>
     * </p>
     * @param companyId UUID de la empresa asociada al token.
     * @param user Usuario asociado al token.
     * @return String con el refresh token plano (único momento en que se entrega al cliente).
     */
    String saveNewRefreshToken(UUID companyId, User user) {
        String refreshTokenPlain = UUID.randomUUID().toString();
        String refreshTokenHash = SecurityUtil.sha256Hex(refreshTokenPlain);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setCompanyId(companyId);
        refreshToken.setTokenHash(refreshTokenHash);
        refreshToken.setIssuedAt(Instant.now());
        refreshToken.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);
        return refreshTokenPlain;
    }

    /**
     * Actualiza los datos de un refresh token ya persistido (por ejemplo, para marcarlo como revocado).
     * @param refreshToken El token a actualizar.
     */
    void updateRefreshToken(RefreshToken refreshToken) {
        refreshTokenRepository.save(refreshToken);
    }

    /**
     * Revoca todos los refresh tokens asociados a un usuario (ejemplo: logout global).
     * @param userId UUID del usuario.
     */
    void revokeAllByUserId(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    /**
     * Busca un refresh token por su hash, lanzando excepción si no existe.
     * <p>
     * - Útil para validar que el refresh token entregado por el cliente es válido y no revocado/expirado.
     * </p>
     * @param refreshTokenHash Hash SHA-256 del refresh token.
     * @return Entidad RefreshToken si existe.
     * @throws UnauthorizedException si no existe o está revocado/expirado.
     */
    RefreshToken findByTokenHash(String refreshTokenHash) {
        return refreshTokenRepository.findByTokenHash(refreshTokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
    }
}
