package com.valiantech.core.iam.auth.service;

import com.valiantech.core.iam.auth.model.RefreshToken;
import com.valiantech.core.iam.auth.repository.RefreshTokenRepository;
import com.valiantech.core.iam.exception.UnauthorizedException;
import com.valiantech.core.iam.security.SecurityUtil;
import com.valiantech.core.iam.user.model.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock RefreshTokenRepository refreshTokenRepository;
    @InjectMocks
    RefreshTokenService refreshTokenService;

    @Nested
    @DisplayName("saveNewRefreshToken")
    class SaveNewRefreshTokenTests {

        @Test
        @DisplayName("Debe generar, hashear y guardar un refresh token correctamente")
        void shouldSaveHashedRefreshToken() {
            UUID userId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            User user = User.builder().id(userId).build();

            // Mock de SecurityUtil.sha256Hex
            try (MockedStatic<SecurityUtil> util = mockStatic(SecurityUtil.class)) {
                util.when(() -> SecurityUtil.sha256Hex(anyString())).thenReturn("HASHED-TOKEN");

                when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

                String result = refreshTokenService.saveNewRefreshToken(companyId, user);

                assertNotNull(result); // Es el token plano, único momento en que se devuelve
                verify(refreshTokenRepository).save(any(RefreshToken.class));
                util.verify(() -> SecurityUtil.sha256Hex(anyString()), times(1));
            }
        }
    }

    @Nested
    @DisplayName("updateRefreshToken")
    class UpdateRefreshTokenTests {

        @Test
        @DisplayName("Debe guardar el refresh token actualizado")
        void shouldUpdateRefreshToken() {
            RefreshToken token = new RefreshToken();
            refreshTokenService.updateRefreshToken(token);
            verify(refreshTokenRepository).save(token);
        }
    }

    @Nested
    @DisplayName("revokeAllByUserId")
    class RevokeAllByUserIdTests {

        @Test
        @DisplayName("Debe revocar todos los refresh tokens por userId")
        void shouldRevokeAllTokensByUserId() {
            UUID userId = UUID.randomUUID();
            refreshTokenService.revokeAllByUserId(userId);
            verify(refreshTokenRepository).revokeAllByUserId(userId);
        }
    }

    @Nested
    @DisplayName("findByTokenHash")
    class FindByTokenHashTests {

        @Test
        @DisplayName("Debe retornar el refresh token si existe el hash")
        void shouldReturnTokenIfExists() {
            String hash = "hash";
            RefreshToken token = new RefreshToken();
            when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));

            RefreshToken result = refreshTokenService.findByTokenHash(hash);

            assertEquals(token, result);
            verify(refreshTokenRepository).findByTokenHash(hash);
        }

        @Test
        @DisplayName("Debe lanzar UnauthorizedException si el hash no existe")
        void shouldThrowUnauthorizedIfNotExists() {
            String hash = "notfound";
            when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.empty());

            assertThrows(UnauthorizedException.class, () -> refreshTokenService.findByTokenHash(hash));
            verify(refreshTokenRepository).findByTokenHash(hash);
        }
    }
}
