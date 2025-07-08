package com.valiantech.core.iam.company.service;

import static org.junit.jupiter.api.Assertions.*;

import com.valiantech.core.iam.company.model.Company;
import com.valiantech.core.iam.company.model.CompanyStatus;
import com.valiantech.core.iam.company.repository.CompanyRepository;
import com.valiantech.core.iam.company.dto.*;
import com.valiantech.core.iam.exception.ConflictException;
import com.valiantech.core.iam.exception.NotFoundException;
import com.valiantech.core.iam.user.dto.CreateUserRequest;
import com.valiantech.core.iam.user.dto.UserResponse;
import com.valiantech.core.iam.user.model.UserStatus;
import com.valiantech.core.iam.usercompany.service.UserCompanyService;
import com.valiantech.core.iam.user.service.UserService;
import com.valiantech.core.iam.usercompany.model.UserCompanyRole;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock CompanyRepository companyRepository;
    @Mock UserService userService;
    @Mock UserCompanyService userCompanyService;
    @InjectMocks
    CompanyService service;

    final UUID companyId = UUID.randomUUID();
    final String rut = "12345678-9";
    final Instant now = Instant.now();

    @Nested
    @DisplayName("onboarding")
    class OnboardingTests {

        @Test
        @DisplayName("Debe crear empresa, usuario owner y vínculo OWNER cuando RUT es único")
        void shouldCreateCompanyAndOwnerIfRutIsUnique() {
            var companyData = new CreateCompanyRequest(
                    rut,
                    "Empresa SA",
                    "ESA",
                    "Actividades de programacion",
                    "La direccion",
                    "Santiago",
                    "Metropolitana",
                    "email@domain.cl",
                    "999999999",
                    null
            );
            var ownerData = new CreateUserRequest(
                    "JUAN PEREZ RIQUELME",
                    "jperezr@empresasa.com",
                    "password"
            );
            var request = new CompanyOnboardingRequest(companyData, ownerData);

            when(companyRepository.findByRut(rut)).thenReturn(Optional.empty());

            // Simula el save
            Company savedCompany = Company.builder()
                    .id(companyId)
                    .rut(rut)
                    .businessName("Razón")
                    .tradeName("Fantasia")
                    .activity("Actividad")
                    .address("Dir")
                    .commune("Comuna")
                    .region("Región")
                    .email("mail@e.cl")
                    .phone("123")
                    .logoUrl("logo")
                    .status(CompanyStatus.ACTIVE)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            when(companyRepository.save(any(Company.class))).thenReturn(savedCompany);

            var userResponse = new UserResponse(UUID.randomUUID(), "Owner Name", "owner@email.com", false, UserStatus.ACTIVE, null, null, null);
            when(userService.registerActiveUser(ownerData)).thenReturn(userResponse);

            CompanyResponse response = service.onboarding(request);

            assertEquals(savedCompany.getId(), response.id());
            assertEquals(savedCompany.getRut(), response.rut());
            verify(companyRepository, times(1)).save(any(Company.class));
            verify(userService, times(1)).registerActiveUser(ownerData);
            verify(userCompanyService, times(1)).registerUserCompany(userResponse.id(), companyId, UserCompanyRole.OWNER);
        }

        @Test
        @DisplayName("Debe lanzar ConflictException si el RUT ya existe")
        void shouldThrowConflictExceptionIfRutExists() {
            var companyData = new CreateCompanyRequest(
                    rut,
                    "Empresa SA",
                    "ESA",
                    "Actividades de programacion",
                    "La direccion",
                    "Santiago",
                    "Metropolitana",
                    "email@domain.cl",
                    "999999999",
                    null
            );
            var ownerData = new CreateUserRequest(
                    "JUAN PEREZ RIQUELME",
                    "jperezr@empresasa.com",
                    "password"
            );
            var request = new CompanyOnboardingRequest(companyData, ownerData);

            when(companyRepository.findByRut(rut)).thenReturn(Optional.of(mock(Company.class)));

            ConflictException ex = assertThrows(ConflictException.class, () -> service.onboarding(request));
            assertTrue(ex.getMessage().contains("already exists"));
            verify(companyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateCompany")
    class UpdateCompanyTests {

        @Test
        @DisplayName("Debe actualizar solo los campos no nulos del request")
        void shouldUpdateOnlyNonNullFields() {
            Company company = Company.builder()
                    .id(companyId)
                    .rut(rut)
                    .businessName("Original Name")
                    .status(CompanyStatus.ACTIVE)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            UpdateCompanyRequest request = new UpdateCompanyRequest("Nuevo Nombre", null, null, null, null, null, null, null, null, null);

            when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
            when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

            CompanyResponse response = service.updateCompany(companyId, request);

            assertEquals("Nuevo Nombre", response.businessName());
            assertEquals(companyId, response.id());
            verify(companyRepository).save(company);
        }

        @Test
        @DisplayName("Debe lanzar NotFoundException si la empresa no existe")
        void shouldThrowNotFoundExceptionIfCompanyDoesNotExist() {
            UpdateCompanyRequest request = new UpdateCompanyRequest(null, null, null, null, null, null, null, null, null, null);

            when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

            NotFoundException ex = assertThrows(NotFoundException.class, () -> service.updateCompany(companyId, request));
            assertTrue(ex.getMessage().contains("not found"));
            verify(companyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getCompany")
    class GetCompanyTests {

        @Test
        @DisplayName("Debe retornar los datos de la empresa si existe")
        void shouldReturnCompanyDataIfExists() {
            Company company = Company.builder()
                    .id(companyId)
                    .rut(rut)
                    .businessName("Nombre Cía")
                    .status(CompanyStatus.ACTIVE)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

            CompanyResponse response = service.getCompany(companyId);

            assertEquals(companyId, response.id());
            assertEquals("Nombre Cía", response.businessName());
            verify(companyRepository, times(1)).findById(companyId);
        }

        @Test
        @DisplayName("Debe lanzar NotFoundException si la empresa no existe")
        void shouldThrowNotFoundExceptionIfCompanyNotExists() {
            when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

            NotFoundException ex = assertThrows(NotFoundException.class, () -> service.getCompany(companyId));
            assertTrue(ex.getMessage().contains("not found"));
            verify(companyRepository, times(1)).findById(companyId);
        }
    }
}
