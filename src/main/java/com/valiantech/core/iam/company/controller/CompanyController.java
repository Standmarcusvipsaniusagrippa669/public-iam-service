package com.valiantech.core.iam.company.controller;

import com.valiantech.core.iam.company.dto.*;
import com.valiantech.core.iam.company.service.CompanyService;
import com.valiantech.core.iam.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador REST para la gestión de compañías (empresas) dentro del sistema IAM.
 *
 * <p>
 * Expone endpoints para el proceso de onboarding (registro), actualización y obtención de los datos de la empresa
 * asociada al usuario autenticado, utilizando el contexto de seguridad proporcionado por el JWT.
 * </p>
 *
 * <h3>Endpoints:</h3>
 * <ul>
 *   <li>
 *     <b>POST /api/v1/companies/onboarding</b><br>
 *     <b>Descripción:</b> Crea una nueva compañía junto al usuario owner.<br>
 *     <b>Respuesta:</b> 200 OK si la empresa es creada, 409 Conflict si ya existe el RUT.<br>
 *     <b>Requiere autenticación (Bearer JWT).</b>
 *   </li>
 *   <li>
 *     <b>PUT /api/v1/companies/me</b><br>
 *     <b>Descripción:</b> Actualiza los datos de la empresa actual (extraída del token).<br>
 *     <b>Respuesta:</b> 200 OK si la empresa es actualizada, 404 si no existe o no hay acceso.<br>
 *     <b>Solo disponible para OWNER de la empresa.</b>
 *   </li>
 *   <li>
 *     <b>GET /api/v1/companies/me</b><br>
 *     <b>Descripción:</b> Obtiene los datos de la empresa actual para el usuario autenticado.<br>
 *     <b>Respuesta:</b> 200 OK con los datos, 404 si no existe o no hay acceso.<br>
 *     <b>Disponible para OWNER, ADMIN y VIEWER de la empresa.</b>
 *   </li>
 * </ul>
 *
 * <b>Notas:</b>
 * <ul>
 *   <li>El identificador de empresa se extrae del JWT usando un utilitario de seguridad ({@code SecurityUtil.getCompanyIdFromContext()}).</li>
 *   <li>No existen endpoints para listar o modificar empresas arbitrarias por motivos de seguridad multi-tenant.</li>
 *   <li>Las reglas de negocio específicas y validaciones están delegadas al servicio {@link CompanyService}.</li>
 * </ul>
 *
 * @author Ian Cardenas
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
@Tag(name = "Company Management", description = "Operations for managing companies")
public class CompanyController {

    private final CompanyService companyService;

    /**
     * Endpoint para crear una nueva compañía y su usuario owner mediante el proceso de onboarding.
     *
     * @param request Datos requeridos para el registro de la empresa y el usuario owner.
     * @return Respuesta con los datos de la nueva empresa.
     */
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Create a new company",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Company successfully created"),
                    @ApiResponse(responseCode = "409", description = "Company with same RUT already exists")
            }
    )
    @PostMapping("/onboarding")
    public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CompanyOnboardingRequest request) {
        return ResponseEntity.ok(companyService.onboarding(request));
    }

    /**
     * Endpoint para actualizar los datos de la empresa a la que pertenece el usuario autenticado.
     * Solo accesible por usuarios con rol OWNER.
     *
     * @param request Datos de actualización de la empresa.
     * @return Respuesta con los datos actualizados de la empresa.
     */
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Actualizar mi empresa (según el contexto del token)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Empresa actualizada"),
                    @ApiResponse(responseCode = "404", description = "Empresa no encontrada")
            }
    )
    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('OWNER')")
    public ResponseEntity<CompanyResponse> updateMyCompany(@Valid @RequestBody UpdateCompanyRequest request) {
        UUID companyId = SecurityUtil.getCompanyIdFromContext();
        return ResponseEntity.ok(companyService.updateCompany(companyId, request));
    }

    /**
     * Endpoint para obtener los datos de la empresa asociada al usuario autenticado.
     * Accesible para OWNER, ADMIN y VIEWER de la empresa.
     *
     * @return Respuesta con los datos de la empresa.
     */
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Obtener los datos de mi empresa (según el contexto del token)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Empresa encontrada"),
                    @ApiResponse(responseCode = "404", description = "Empresa no encontrada o no tienes acceso")
            }
    )
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','VIEWER')")
    public ResponseEntity<CompanyResponse> getMyCompany() {
        UUID companyId = SecurityUtil.getCompanyIdFromContext();
        return ResponseEntity.ok(companyService.getCompany(companyId));
    }
}
