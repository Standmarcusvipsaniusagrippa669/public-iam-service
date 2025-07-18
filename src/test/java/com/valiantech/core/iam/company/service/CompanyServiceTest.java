package com.valiantech.core.iam.company.service;

import static org.junit.jupiter.api.Assertions.*;

import com.valiantech.core.iam.audit.service.UserAuditLogService;
import com.valiantech.core.iam.auth.service.JwtService;
import com.valiantech.core.iam.clients.CompanyClient;
import com.valiantech.core.iam.company.model.CompanyStatus;
import com.valiantech.core.iam.company.dto.*;
import com.valiantech.core.iam.exception.ConflictException;
import com.valiantech.core.iam.exception.NotFoundException;
import com.valiantech.core.iam.security.SecurityUtil;
import com.valiantech.core.iam.user.dto.CreateUserRequest;
import com.valiantech.core.iam.user.dto.UserResponse;
import com.valiantech.core.iam.user.model.User;
import com.valiantech.core.iam.user.model.UserStatus;
import com.valiantech.core.iam.usercompany.service.UserCompanyService;
import com.valiantech.core.iam.user.service.UserService;
import com.valiantech.core.iam.usercompany.model.UserCompanyRole;
import com.valiantech.core.iam.util.ClientInfoService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    CompanyClient companyClient;
    @Mock UserService userService;
    @Mock UserCompanyService userCompanyService;
    @Mock
    UserAuditLogService userAuditLogService;
    @Mock
    ClientInfoService clientInfoService;
    @Mock
    JwtService jwtService;
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

            // Simula que NO existe la empresa
            when(jwtService.generateServiceToken(eq("company:read"), any())).thenReturn("read-token");
            //when(companyClient.findByRut(eq(rut), anyString())).thenReturn(null); // <--- Cambia esto a NULL para simular que NO existe

            // Simula creación exitosa de empresa
            when(jwtService.generateServiceToken(eq("company:create"), any())).thenReturn("create-token");
            CompanyResponse savedCompany = new CompanyResponse(
                    companyId,
                    rut,
                    "Razón",
                    "Fantasia",
                    "Actividad",
                    "Dir",
                    "Comuna",
                    "Región",
                    "mail@e.cl",
                    "123",
                    "logo",
                    CompanyStatus.ACTIVE,
                    now,
                    now
            );
            when(companyClient.createCompany(any(CreateCompanyRequest.class), anyString())).thenReturn(savedCompany);

            // Simula usuario y vínculo
            UserResponse userResponse = new UserResponse(UUID.randomUUID(), "Owner Name", "owner@email.com", false, UserStatus.ACTIVE, null, null, null);
            when(userService.registerActiveUser(ownerData)).thenReturn(userResponse);

            doNothing().when(userAuditLogService).logAsync(any());
            when(clientInfoService.getClientIp()).thenReturn("0.0.0.0");
            when(clientInfoService.getCookies()).thenReturn(null);
            when(clientInfoService.getUserAgent()).thenReturn("Test");

            when(companyClient.findByRut(eq(rut), anyString())).thenReturn(null);

            CompanyResponse response = service.onboarding(request);

            assertEquals(savedCompany.id(), response.id());
            assertEquals(savedCompany.rut(), response.rut());
            verify(companyClient, times(1)).createCompany(any(CreateCompanyRequest.class), anyString());
            verify(userService, times(1)).registerActiveUser(ownerData);
            verify(userCompanyService, times(1)).registerUserCompany(userResponse.id(), companyId, UserCompanyRole.OWNER);
            verify(userAuditLogService, times(1)).logAsync(any());
        }

        @Test
        @DisplayName("Debe lanzar ConflictException si la empresa YA existe para el rut")
        void shouldThrowConflictIfCompanyAlreadyExists() {
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

            when(jwtService.generateServiceToken(eq("company:read"), any())).thenReturn("read-token");
            // Aquí SÍ existe la empresa: findByRut retorna un objeto NO nulo
            when(companyClient.findByRut(eq(rut), anyString())).thenReturn(
                    new CompanyResponse(companyId, rut, "Razón", "Fantasia", "Actividad", "Dir", "Comuna", "Región", "mail@e.cl", "123", "logo", CompanyStatus.ACTIVE, now, now)
            );

            ConflictException ex = assertThrows(ConflictException.class, () -> service.onboarding(request));
            assertTrue(ex.getMessage().contains("Company with this RUT already exists."));

            verify(companyClient, never()).createCompany(any(), any());
            verify(userService, never()).registerActiveUser(any());
            verify(userCompanyService, never()).registerUserCompany(any(), any(), any());
            verify(userAuditLogService, never()).logAsync(any());
        }
    }

    @Nested
    @DisplayName("updateCompany")
    class UpdateCompanyTests {

        @Test
        @DisplayName("Debe actualizar solo los campos no nulos del request")
        void shouldUpdateOnlyNonNullFields() {
            CompanyResponse savedCompany = new CompanyResponse(
                    companyId,
                    rut,
                    "Razón",
                    "Fantasia",
                    "Actividad",
                    "Dir",
                    "Comuna",
                    "Región",
                    "mail@e.cl",
                    "123",
                    "logo",
                    CompanyStatus.ACTIVE,
                    now,
                    now
            );

            UpdateCompanyRequest request = new UpdateCompanyRequest("Nuevo Nombre", null, null, null, null, null, null, null, null, null);

            // Mock token y update
            when(jwtService.generateServiceTokenWithIdentifications(any(), any(), eq("company:update"), any())).thenReturn("tok");
            when(companyClient.update(request, "tok")).thenReturn(
                    new CompanyResponse(
                            companyId,
                            rut,
                            "Nuevo Nombre", // Nuevo valor
                            "Fantasia",
                            "Actividad",
                            "Dir",
                            "Comuna",
                            "Región",
                            "mail@e.cl",
                            "123",
                            "logo",
                            CompanyStatus.ACTIVE,
                            now,
                            now
                    )
            );

            doNothing().when(userAuditLogService).logAsync(any());
            when(clientInfoService.getClientIp()).thenReturn("0.0.0.0");
            when(clientInfoService.getCookies()).thenReturn(null);
            when(clientInfoService.getUserAgent()).thenReturn("Test");

            try (var mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getUserIdFromContext).thenReturn(UUID.randomUUID());
                CompanyResponse response = service.updateCompany(companyId, request);

                assertEquals("Nuevo Nombre", response.businessName());
                assertEquals(companyId, response.id());
                verify(companyClient).update(request, "tok");
                verify(userAuditLogService).logAsync(any());
            }
        }

        @Test
        @DisplayName("Debe lanzar NotFoundException si la empresa no existe")
        void shouldThrowNotFoundExceptionIfCompanyDoesNotExist() {
            UpdateCompanyRequest request = new UpdateCompanyRequest(null, null, null, null, null, null, null, null, null, null);

            // Simula que el client lanza NotFoundException
            when(jwtService.generateServiceTokenWithIdentifications(any(), any(), eq("company:update"), any())).thenReturn("tok");
            when(companyClient.update(request, "tok")).thenThrow(new NotFoundException("not found"));

            try (var mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getUserIdFromContext).thenReturn(UUID.randomUUID());
                NotFoundException ex = assertThrows(NotFoundException.class, () -> service.updateCompany(companyId, request));
                assertTrue(ex.getMessage().contains("not found"));
                verify(companyClient).update(request, "tok");
                verify(userAuditLogService, never()).logAsync(any());
            }
        }
    }

    @Nested
    @DisplayName("getCompany")
    class GetCompanyTests {

        @Test
        @DisplayName("Debe retornar los datos de la empresa si existe")
        void shouldReturnCompanyDataIfExists() {
            CompanyResponse savedCompany = new CompanyResponse(
                    companyId,
                    rut,
                    "Razón",
                    "Fantasia",
                    "Actividad",
                    "Dir",
                    "Comuna",
                    "Región",
                    "mail@e.cl",
                    "123",
                    "logo",
                    CompanyStatus.ACTIVE,
                    now,
                    now
            );

            String expectedToken = "mock-token";

            // Mock estático para SecurityUtil
            try (var mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getUserIdFromContext).thenReturn(UUID.randomUUID());
                when(jwtService.generateServiceTokenWithIdentifications(any(), eq(companyId), eq("company:read"), any(Duration.class)))
                        .thenReturn(expectedToken);
                when(companyClient.findMeCompany(expectedToken)).thenReturn(savedCompany);

                CompanyResponse response = service.getCompany(companyId);

                assertEquals(companyId, response.id());
                assertEquals("Razón", response.businessName());
                verify(companyClient, times(1)).findMeCompany(expectedToken);
                verify(jwtService, times(1)).generateServiceTokenWithIdentifications(any(), eq(companyId), eq("company:read"), any(Duration.class));
            }
        }

        @Test
        @DisplayName("Debe lanzar NotFoundException si la empresa no existe")
        void shouldThrowNotFoundExceptionIfCompanyNotExists() {
            String expectedToken = "mock-token";

            try (var mocked = mockStatic(SecurityUtil.class)) {
                mocked.when(SecurityUtil::getUserIdFromContext).thenReturn(UUID.randomUUID());
                when(jwtService.generateServiceTokenWithIdentifications(any(), eq(companyId), eq("company:read"), any(Duration.class)))
                        .thenReturn(expectedToken);
                when(companyClient.findMeCompany(expectedToken)).thenReturn(null);

                NotFoundException ex = assertThrows(NotFoundException.class, () -> service.getCompany(companyId));
                assertTrue(ex.getMessage().contains("not found"));
                verify(companyClient, times(1)).findMeCompany(expectedToken);
                verify(jwtService, times(1)).generateServiceTokenWithIdentifications(any(), eq(companyId), eq("company:read"), any(Duration.class));
            }
        }
    }
}
