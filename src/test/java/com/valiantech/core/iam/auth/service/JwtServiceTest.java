package com.valiantech.core.iam.auth.service;

import com.valiantech.core.iam.user.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    JwtService jwtService;
    String secret = "my-super-secret-that-is-long-enough-to-be-valid-for-hmac";

    User user = User.builder()
            .id(UUID.randomUUID())
            .email("test@email.com")
            .build();

    UUID companyId = UUID.randomUUID();
    String role = "ADMIN";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Inyecta el secret "a mano" porque @Value no se procesa en test unitario puro
        var secretField = JwtService.class.getDeclaredFields()[0];
        secretField.setAccessible(true);
        try {
            secretField.set(jwtService, secret);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Debe generar y parsear correctamente el JWT con los claims requeridos")
    void shouldGenerateAndParseJwtWithCorrectClaims() {
        String token = jwtService.generateToken(user, companyId, role);

        Claims claims = jwtService.parseToken(token);

        assertEquals(user.getId().toString(), claims.getSubject());
        assertEquals(user.getEmail(), claims.get("email"));
        assertEquals(companyId.toString(), claims.get("companyId"));
        assertEquals(role, claims.get("role"));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el token está firmado con otro secret")
    void shouldFailOnTokenWithInvalidSecret() {
        String token = jwtService.generateToken(user, companyId, role);

        // Crea un nuevo servicio con otro secret
        JwtService otherJwtService = new JwtService();
        var secretField = JwtService.class.getDeclaredFields()[0];
        secretField.setAccessible(true);
        try {
            secretField.set(otherJwtService, "otro-secret-totalmente-distinto-y-largo-123456789");
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        assertThrows(JwtException.class, () -> otherJwtService.parseToken(token));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el token está mal formado")
    void shouldFailOnMalformedToken() {
        String invalidToken = "this.is.not.a.jwt";
        assertThrows(JwtException.class, () -> jwtService.parseToken(invalidToken));
    }

}

