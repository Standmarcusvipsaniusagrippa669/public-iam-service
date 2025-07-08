package com.valiantech.core.iam.user.service;

import static org.junit.jupiter.api.Assertions.*;

import com.valiantech.core.iam.exception.ConflictException;
import com.valiantech.core.iam.exception.NotFoundException;
import com.valiantech.core.iam.exception.UnauthorizedException;
import com.valiantech.core.iam.user.dto.*;
import com.valiantech.core.iam.user.model.User;
import com.valiantech.core.iam.user.model.UserStatus;
import com.valiantech.core.iam.user.repository.UserRepository;
import com.valiantech.core.iam.usercompany.model.UserCompany;
import com.valiantech.core.iam.usercompany.service.UserCompanyService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock UserCompanyService userCompanyService;
    @InjectMocks
    UserService service;

    final UUID userId = UUID.randomUUID();
    final UUID companyId = UUID.randomUUID();
    final String email = "test@email.com";
    final String password = "clave123";

    @Nested
    @DisplayName("registerActiveUser")
    class RegisterActiveUserTests {

        @Test
        @DisplayName("Debe registrar usuario activo si el email es único")
        void shouldRegisterUserIfEmailUnique() {
            CreateUserRequest req = new CreateUserRequest("Nuevo", email, password);

            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(password)).thenReturn("hashed");
            User saved = User.builder().id(userId).fullName("Nuevo").email(email).passwordHash("hashed").emailValidated(true)
                    .mustChangePassword(false).status(UserStatus.ACTIVE).createdAt(Instant.now()).updatedAt(Instant.now()).build();
            when(userRepository.save(any(User.class))).thenReturn(saved);

            UserResponse response = service.registerActiveUser(req);

            assertEquals(userId, response.id());
            assertEquals("Nuevo", response.fullName());
            assertEquals(email, response.email());
            assertEquals(UserStatus.ACTIVE, response.status());
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Debe lanzar ConflictException si el email ya existe")
        void shouldThrowConflictIfEmailExists() {
            CreateUserRequest req = new CreateUserRequest("Dup", email, password);
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(mock(User.class)));

            assertThrows(ConflictException.class, () -> service.registerActiveUser(req));
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUserTests {

        @Test
        @DisplayName("Debe actualizar datos si usuario y vínculo existen")
        void shouldUpdateUserIfExists() {
            UpdateUserRequest req = new UpdateUserRequest("Name Changed", email, UserStatus.DISABLED, true);
            UserCompany userCompany = UserCompany.builder().userId(userId).companyId(companyId).build();
            User user = User.builder()
                    .id(userId)
                    .fullName("Old Name")
                    .email(email)
                    .mustChangePassword(false)
                    .status(UserStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(userCompanyService.getUserCompany(userId, companyId)).thenReturn(Optional.of(userCompany));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserResponse response = service.updateUser(userId, companyId, req);

            assertEquals("Name Changed", response.fullName());
            assertEquals(email, response.email());
            assertEquals(UserStatus.DISABLED, response.status());
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Debe lanzar UnauthorizedException si vínculo usuario-empresa no existe")
        void shouldThrowUnauthorizedIfNoUserCompany() {
            UpdateUserRequest req = new UpdateUserRequest("Any", email, UserStatus.ACTIVE, false);
            when(userCompanyService.getUserCompany(userId, companyId)).thenReturn(Optional.empty());

            assertThrows(UnauthorizedException.class, () -> service.updateUser(userId, companyId, req));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar NotFoundException si el usuario no existe")
        void shouldThrowNotFoundIfUserNotExists() {
            UpdateUserRequest req = new UpdateUserRequest("Any", email, UserStatus.ACTIVE, false);
            UserCompany userCompany = UserCompany.builder().userId(userId).companyId(companyId).build();

            when(userCompanyService.getUserCompany(userId, companyId)).thenReturn(Optional.of(userCompany));
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.updateUser(userId, companyId, req));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar ConflictException si el email nuevo ya pertenece a otro usuario")
        void shouldThrowConflictIfEmailAlreadyRegistered() {
            UpdateUserRequest req = new UpdateUserRequest("Cualquiera", "otro@email.com", UserStatus.ACTIVE, false);
            UserCompany userCompany = UserCompany.builder().userId(userId).companyId(companyId).build();
            User user = User.builder().id(userId).fullName("Name").email(email).status(UserStatus.ACTIVE).mustChangePassword(false).build();
            User another = User.builder().id(UUID.randomUUID()).fullName("Otro").email("otro@email.com").status(UserStatus.ACTIVE).mustChangePassword(false).build();

            when(userCompanyService.getUserCompany(userId, companyId)).thenReturn(Optional.of(userCompany));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.findByEmail("otro@email.com")).thenReturn(Optional.of(another));

            assertThrows(ConflictException.class, () -> service.updateUser(userId, companyId, req));
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getUser")
    class GetUserTests {

        @Test
        @DisplayName("Debe retornar usuario si existe")
        void shouldReturnUserIfExists() {
            User user = User.builder().id(userId).fullName("Nombre").email(email).status(UserStatus.ACTIVE).emailValidated(true).build();
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            UserResponse response = service.getUser(userId);

            assertEquals(userId, response.id());
            assertEquals("Nombre", response.fullName());
        }

        @Test
        @DisplayName("Debe lanzar NotFoundException si el usuario no existe")
        void shouldThrowNotFoundIfUserNotExists() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.getUser(userId));
        }
    }

    @Nested
    @DisplayName("getUserByCompanyId")
    class GetUserByCompanyIdTests {

        @Test
        @DisplayName("Debe retornar usuario si vínculo existe")
        void shouldReturnUserIfLinkExists() {
            UserCompany userCompany = UserCompany.builder().userId(userId).companyId(companyId).build();
            User user = User.builder().id(userId).fullName("Nombre").email(email).status(UserStatus.ACTIVE).emailValidated(true).build();

            when(userCompanyService.getUserCompany(userId, companyId)).thenReturn(Optional.of(userCompany));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            UserResponse response = service.getUserByCompanyId(userId, companyId);

            assertEquals(userId, response.id());
            assertEquals("Nombre", response.fullName());
        }

        @Test
        @DisplayName("Debe lanzar UnauthorizedException si vínculo usuario-empresa no existe")
        void shouldThrowUnauthorizedIfNoLink() {
            when(userCompanyService.getUserCompany(userId, companyId)).thenReturn(Optional.empty());

            assertThrows(UnauthorizedException.class, () -> service.getUserByCompanyId(userId, companyId));
        }

        @Test
        @DisplayName("Debe lanzar NotFoundException si el usuario no existe")
        void shouldThrowNotFoundIfUserNotExists() {
            UserCompany userCompany = UserCompany.builder().userId(userId).companyId(companyId).build();
            when(userCompanyService.getUserCompany(userId, companyId)).thenReturn(Optional.of(userCompany));
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.getUserByCompanyId(userId, companyId));
        }
    }

    @Nested
    @DisplayName("listAll")
    class ListAllTests {

        @Test
        @DisplayName("Debe retornar lista de usuarios de la empresa")
        void shouldReturnUserList() {
            List<UUID> ids = List.of(userId, UUID.randomUUID());
            User user1 = User.builder().id(userId).fullName("User1").email("user1@email.com").status(UserStatus.ACTIVE).emailValidated(true).build();
            User user2 = User.builder().id(ids.get(1)).fullName("User2").email("user2@email.com").status(UserStatus.DISABLED).emailValidated(true).build();

            when(userCompanyService.fetchUsersIdsByCompanyId(companyId)).thenReturn(ids);
            when(userRepository.findAllByIdIn(ids)).thenReturn(List.of(user1, user2));

            List<UserResponse> responses = service.listAll(companyId);

            assertEquals(2, responses.size());
            assertEquals("User1", responses.get(0).fullName());
            assertEquals("User2", responses.get(1).fullName());
        }
    }
}
