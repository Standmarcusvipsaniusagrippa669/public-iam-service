package com.valiantech.core.iam.util;

import static org.junit.jupiter.api.Assertions.*;

import com.valiantech.core.iam.company.dto.CompanyResponse;
import com.valiantech.core.iam.company.model.CompanyStatus;
import com.valiantech.core.iam.exception.ConflictException;
import com.valiantech.core.iam.user.dto.UserResponse;
import com.valiantech.core.iam.user.model.UserStatus;
import com.valiantech.core.iam.usercompany.model.UserCompany;
import com.valiantech.core.iam.usercompany.model.UserCompanyRole;
import org.junit.jupiter.api.*;

import java.util.UUID;

class ValidationUtilsTest {

    @Nested
    @DisplayName("validateCompanyIsActive")
    class ValidateCompanyIsActiveTests {

        @Test
        @DisplayName("Debe permitir si la empresa está activa")
        void shouldAllowIfCompanyActive() {
            CompanyResponse active = new CompanyResponse(UUID.randomUUID(), "rut", "bn", "tn", "act", "addr", "com", "reg", "email", "fono", "logo", CompanyStatus.ACTIVE, null, null);
            assertDoesNotThrow(() -> ValidationUtils.validateCompanyIsActive(active));
        }

        @Test
        @DisplayName("Debe lanzar ConflictException si la empresa NO está activa")
        void shouldThrowIfCompanyNotActive() {
            CompanyResponse inactive = new CompanyResponse(UUID.randomUUID(), "rut", "bn", "tn", "act", "addr", "com", "reg", "email", "fono", "logo", CompanyStatus.INACTIVE, null, null);
            ConflictException ex = assertThrows(ConflictException.class, () -> ValidationUtils.validateCompanyIsActive(inactive));
            assertTrue(ex.getMessage().contains("Company is not active"));
        }
    }

    @Nested
    @DisplayName("validateUserIsActive")
    class ValidateUserIsActiveTests {

        @Test
        @DisplayName("Debe permitir si el usuario está activo")
        void shouldAllowIfUserActive() {
            UserResponse active = new UserResponse(UUID.randomUUID(), "User", "mail", true, UserStatus.ACTIVE, null, null, null);
            assertDoesNotThrow(() -> ValidationUtils.validateUserIsActive(active));
        }

        @Test
        @DisplayName("Debe lanzar ConflictException si el usuario NO está activo")
        void shouldThrowIfUserNotActive() {
            UserResponse inactive = new UserResponse(UUID.randomUUID(), "User", "mail", true, UserStatus.DISABLED, null, null, null);
            ConflictException ex = assertThrows(ConflictException.class, () -> ValidationUtils.validateUserIsActive(inactive));
            assertTrue(ex.getMessage().contains("User is not active"));
        }
    }

    @Nested
    @DisplayName("validateUserHasRole")
    class ValidateUserHasRoleTests {

        @Test
        @DisplayName("Debe permitir si el usuario tiene uno de los roles requeridos")
        void shouldAllowIfUserHasRequiredRole() {
            UserCompany userCompany = UserCompany.builder()
                    .role(UserCompanyRole.ADMIN)
                    .build();

            assertDoesNotThrow(() -> ValidationUtils.validateUserHasRole(userCompany, UserCompanyRole.OWNER, UserCompanyRole.ADMIN));
        }

        @Test
        @DisplayName("Debe lanzar ConflictException si el usuario NO tiene rol permitido")
        void shouldThrowIfUserHasNotRole() {
            UserCompany userCompany = UserCompany.builder()
                    .role(UserCompanyRole.VIEWER)
                    .build();

            ConflictException ex = assertThrows(ConflictException.class, () ->
                    ValidationUtils.validateUserHasRole(userCompany, UserCompanyRole.OWNER, UserCompanyRole.ADMIN));
            assertTrue(ex.getMessage().contains("User does not have required permission"));
        }
    }
}
