package com.valiantech.core.iam.auth.service;

import com.valiantech.core.iam.user.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Servicio encargado de la generación y validación de tokens JWT para autenticación y autorización en la aplicación.
 *
 * <p>
 * Los tokens generados incluyen información del usuario, la empresa seleccionada (companyId) y el rol actual,
 * permitiendo un control granular y seguro sobre los recursos en ambientes multiempresa.
 * </p>
 *
 * <b>Notas de seguridad:</b>
 * <ul>
 *   <li>La clave secreta para firmar los JWT se obtiene desde la propiedad {@code jwt.secret}.</li>
 *   <li>El authToken tiene una expiración de 24 horas desde su emisión.</li>
 *   <li>El algoritmo de firma utilizado es HS256.</li>
 * </ul>
 *
 * <h3>Claims incluidos en el JWT:</h3>
 * <ul>
 *   <li><b>sub:</b> ID del usuario autenticado</li>
 *   <li><b>email:</b> Email del usuario</li>
 *   <li><b>companyId:</b> Empresa en contexto</li>
 *   <li><b>role:</b> Rol del usuario en esa empresa</li>
 * </ul>
 *
 * @author Ian Cardenas
 * @since 1.0
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    /**
     * Genera un JWT firmado para el usuario, empresa y rol indicados.
     *
     * @param user      Usuario autenticado
     * @param companyId Empresa seleccionada
     * @param role      Rol del usuario en la empresa
     * @return Token JWT firmado con los claims requeridos
     */
    public String generateToken(User user, UUID companyId, String role) {
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("companyId", companyId.toString())
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Parsea y valida un authToken JWT, devolviendo los claims incluidos.
     *
     * @param token Token JWT firmado
     * @return Claims (reclamos) incluidos en el JWT
     * @throws io.jsonwebtoken.JwtException Si el authToken es inválido o está expirado
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String generateServiceToken(String scope, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claim("client_type", "service")
                .claim("client_id", "iam-service")
                .claim("scope", scope)
                .setId(UUID.randomUUID().toString()) // jti: single use
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(ttl)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public String generateServiceTokenWithIdentifications(UUID userId, UUID companyId, String scope, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claim("client_type", "service")
                .claim("client_id", "iam-service")
                .claim("user_id", userId)
                .claim("company_id", companyId)
                .claim("scope", scope)
                .setId(UUID.randomUUID().toString()) // jti: single use
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(ttl)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
