package com.valiantech.core.iam.company.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.valiantech.core.iam.company.dto.*;
import com.valiantech.core.iam.company.model.CompanyStatus;
import com.valiantech.core.iam.company.service.CompanyService;
import com.valiantech.core.iam.exception.ConflictException;
import com.valiantech.core.iam.exception.NotFoundException;
import com.valiantech.core.iam.security.SecurityUtil;
import com.valiantech.core.iam.user.dto.CreateUserRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyControllerUnitTest {

    @Mock
    CompanyService companyService;

    @InjectMocks
    CompanyController controller;

    @Nested
    @DisplayName("POST /api/v1/companies/onboarding")
    class CreateCompanyTests {

        @Test
        @DisplayName("Debe retornar 200 y datos de la compañía si es creada exitosamente")
        void shouldReturn200AndCompanyOnSuccess() {
            CompanyOnboardingRequest request = new CompanyOnboardingRequest(
                    new CreateCompanyRequest(
                            "11111111-1",
                            "Empresa SA",
                            "ESA",
                            "Actividades de programacion",
                            "La direccion",
                            "Santiago",
                            "Metropolitana",
                            "email@domain.cl",
                            "999999999",
                            null
                    ),
                    new CreateUserRequest(
                            "JUAN PEREZ RIQUELME",
                            "jperezr@empresasa.com",
                            "password"
                    )
            );
            CompanyResponse response = new CompanyResponse(
                    UUID.randomUUID(),
                    "11111111-1",
                    "Empresa SA",
                    "ESA",
                    "Actividades de programacion",
                    "La direccion",
                    "Santiago",
                    "Metropolitana",
                    "email@domain.cl",
                    "999999999",
                    null,
                    CompanyStatus.ACTIVE,
                    Instant.now(),
                    Instant.now()
            );

            when(companyService.onboarding(request)).thenReturn(response);

            var result = controller.create(request);

            assertEquals(200, result.getStatusCode().value());
            assertEquals(response, result.getBody());
            verify(companyService, times(1)).onboarding(request);
        }

        @Test
        @DisplayName("Debe lanzar ConflictException si el RUT ya existe")
        void shouldThrowConflictExceptionIfRutExists() {
            CompanyOnboardingRequest request = new CompanyOnboardingRequest(
                    new CreateCompanyRequest(
                            "11111111-1",
                            "Empresa SA",
                            "ESA",
                            "Actividades de programacion",
                            "La direccion",
                            "Santiago",
                            "Metropolitana",
                            "email@domain.cl",
                            "999999999",
                            null
                    ),
                    new CreateUserRequest(
                            "JUAN PEREZ RIQUELME",
                            "jperezr@empresasa.com",
                            "password"
                    )
            );

            when(companyService.onboarding(request))
                    .thenThrow(new ConflictException("RUT ya registrado"));

            ConflictException ex = assertThrows(ConflictException.class, () -> controller.create(request));
            assertTrue(ex.getMessage().contains("RUT ya registrado"));
            verify(companyService, times(1)).onboarding(request);
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/companies/me")
    class UpdateCompanyTests {

        @Test
        @DisplayName("Debe retornar 200 y datos de la compañía actualizada si el usuario es OWNER")
        void shouldReturn200AndUpdatedCompanyForOwner() {
            UUID companyId = UUID.randomUUID();
            UpdateCompanyRequest request = new UpdateCompanyRequest(
                    "Empresa SA",
                    "ESA",
                    "Actividades de programacion",
                    "La direccion",
                    "Santiago",
                    "Metropolitana",
                    "email@domain.cl",
                    "999999999",
                    null,
                    CompanyStatus.ACTIVE
            );
            CompanyResponse response = new CompanyResponse(
                    UUID.randomUUID(),
                    "11111111-1",
                    "Empresa SA",
                    "ESA",
                    "Actividades de programacion",
                    "La direccion",
                    "Santiago",
                    "Metropolitana",
                    "email@domain.cl",
                    "999999999",
                    null,
                    CompanyStatus.ACTIVE,
                    Instant.now(),
                    Instant.now()
            );

            // Suponiendo que puedes mockear el método estático
            try (var mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCompanyIdFromContext).thenReturn(companyId);
                when(companyService.updateCompany(companyId, request)).thenReturn(response);

                var result = controller.updateMyCompany(request);

                assertEquals(200, result.getStatusCode().value());
                assertEquals(response, result.getBody());
                verify(companyService, times(1)).updateCompany(companyId, request);
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/companies/me")
    class GetCompanyTests {

        @Test
        @DisplayName("Debe retornar 200 y datos de la compañía si existe y el usuario tiene acceso")
        void shouldReturn200AndCompanyIfExists() {
            UUID companyId = UUID.randomUUID();
            CompanyResponse response = new CompanyResponse(
                    UUID.randomUUID(),
                    "11111111-1",
                    "Empresa SA",
                    "ESA",
                    "Actividades de programacion",
                    "La direccion",
                    "Santiago",
                    "Metropolitana",
                    "email@domain.cl",
                    "999999999",
                    null,
                    CompanyStatus.ACTIVE,
                    Instant.now(),
                    Instant.now()
            );

            try (var mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCompanyIdFromContext).thenReturn(companyId);
                when(companyService.getCompany(companyId)).thenReturn(response);

                var result = controller.getMyCompany();

                assertEquals(200, result.getStatusCode().value());
                assertEquals(response, result.getBody());
                verify(companyService, times(1)).getCompany(companyId);
            }
        }

        @Test
        @DisplayName("Debe lanzar NotFoundException si la compañía no existe o no tiene acceso")
        void shouldThrowNotFoundExceptionIfCompanyNotExists() {
            UUID companyId = UUID.randomUUID();

            try (var mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getCompanyIdFromContext).thenReturn(companyId);
                when(companyService.getCompany(companyId)).thenThrow(new NotFoundException("No existe"));

                NotFoundException ex = assertThrows(NotFoundException.class, () -> controller.getMyCompany());
                assertTrue(ex.getMessage().contains("No existe"));
                verify(companyService, times(1)).getCompany(companyId);
            }
        }
    }
}
