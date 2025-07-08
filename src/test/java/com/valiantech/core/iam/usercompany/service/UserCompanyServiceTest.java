package com.valiantech.core.iam.usercompany.service;

import static org.junit.jupiter.api.Assertions.*;

import com.valiantech.core.iam.exception.ConflictException;
import com.valiantech.core.iam.usercompany.model.UserCompany;
import com.valiantech.core.iam.usercompany.model.UserCompanyRole;
import com.valiantech.core.iam.usercompany.model.UserCompanyStatus;
import com.valiantech.core.iam.usercompany.repository.UserCompanyRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCompanyServiceTest {

    @Mock
    UserCompanyRepository userCompanyRepository;

    @InjectMocks
    UserCompanyService service;

    UUID userId = UUID.randomUUID();
    UUID companyId = UUID.randomUUID();
    UUID invitedBy = UUID.randomUUID();

    @Nested
    @DisplayName("registerUserCompanyByInvitation")
    class RegisterByInvitationTests {

        @Test
        @DisplayName("Debe registrar usuario en compañía por invitación si no está vinculado")
        void shouldRegisterUserByInvitationIfNotLinked() {
            when(userCompanyRepository.findByUserIdAndCompanyId(userId, companyId)).thenReturn(Optional.empty());
            when(userCompanyRepository.save(any(UserCompany.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserCompany result = service.registerUserCompanyByInvitation(userId, companyId, UserCompanyRole.ADMIN, invitedBy);

            assertNotNull(result);
            assertEquals(userId, result.getUserId());
            assertEquals(companyId, result.getCompanyId());
            assertEquals(UserCompanyRole.ADMIN, result.getRole());
            assertEquals(invitedBy, result.getInvitedBy());
            assertEquals(UserCompanyStatus.ACTIVE, result.getStatus());
            verify(userCompanyRepository).save(any(UserCompany.class));
        }

        @Test
        @DisplayName("Debe lanzar excepción si el usuario ya está vinculado (por invitación)")
        void shouldThrowIfUserAlreadyLinkedByInvitation() {
            when(userCompanyRepository.findByUserIdAndCompanyId(userId, companyId))
                    .thenReturn(Optional.of(mock(UserCompany.class)));

            assertThrows(ConflictException.class, () ->
                    service.registerUserCompanyByInvitation(userId, companyId, UserCompanyRole.ADMIN, invitedBy)
            );
            verify(userCompanyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("registerUserCompany")
    class RegisterDirectTests {

        @Test
        @DisplayName("Debe registrar usuario en compañía si no está vinculado (sin invitador)")
        void shouldRegisterUserIfNotLinked() {
            when(userCompanyRepository.findByUserIdAndCompanyId(userId, companyId)).thenReturn(Optional.empty());
            when(userCompanyRepository.save(any(UserCompany.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserCompany result = service.registerUserCompany(userId, companyId, UserCompanyRole.OWNER);

            assertNotNull(result);
            assertEquals(userId, result.getUserId());
            assertEquals(companyId, result.getCompanyId());
            assertEquals(UserCompanyRole.OWNER, result.getRole());
            assertNull(result.getInvitedBy());
            assertEquals(UserCompanyStatus.ACTIVE, result.getStatus());
            verify(userCompanyRepository).save(any(UserCompany.class));
        }

        @Test
        @DisplayName("Debe lanzar excepción si el usuario ya está vinculado (directo)")
        void shouldThrowIfUserAlreadyLinkedDirect() {
            when(userCompanyRepository.findByUserIdAndCompanyId(userId, companyId))
                    .thenReturn(Optional.of(mock(UserCompany.class)));

            assertThrows(ConflictException.class, () ->
                    service.registerUserCompany(userId, companyId, UserCompanyRole.OWNER)
            );
            verify(userCompanyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getUserCompany")
    class GetUserCompanyTests {
        @Test
        @DisplayName("Debe retornar UserCompany si existe")
        void shouldReturnUserCompanyIfExists() {
            UserCompany uc = mock(UserCompany.class);
            when(userCompanyRepository.findByUserIdAndCompanyId(userId, companyId)).thenReturn(Optional.of(uc));

            Optional<UserCompany> result = service.getUserCompany(userId, companyId);

            assertTrue(result.isPresent());
            assertEquals(uc, result.get());
        }

        @Test
        @DisplayName("Debe retornar vacío si no existe vínculo")
        void shouldReturnEmptyIfNoLink() {
            when(userCompanyRepository.findByUserIdAndCompanyId(userId, companyId)).thenReturn(Optional.empty());

            Optional<UserCompany> result = service.getUserCompany(userId, companyId);

            assertFalse(result.isPresent());
        }
    }

    @Nested
    @DisplayName("fetchUsersIdsByCompanyId")
    class FetchUserIdsTests {
        @Test
        @DisplayName("Debe retornar lista de IDs de usuarios para la compañía")
        void shouldReturnUserIdsForCompany() {
            List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());
            when(userCompanyRepository.findUserIdByCompanyId(companyId)).thenReturn(ids);

            List<UUID> result = service.fetchUsersIdsByCompanyId(companyId);

            assertEquals(ids, result);
        }
    }
}
