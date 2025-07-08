package com.valiantech.core.iam.company.controller;

import com.valiantech.core.iam.company.dto.*;
import com.valiantech.core.iam.company.service.CompanyService;
import com.valiantech.core.iam.exception.ErrorResponse;
import com.valiantech.core.iam.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
            summary = "Crea una nueva compañía y su usuario owner mediante el proceso de onboarding",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Company successfully created",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CompanyResponse.class),
                                    examples = @ExampleObject(
                                            name = "Token",
                                            summary = "Se obtiene correctamente el token",
                                            value = """
                                                    {
                                                        "id": "26823bec-b626-4547-956e-3bcae0352d13",
                                                        "rut": "77180451-9",
                                                        "businessName": "INVERSIONES VALIAN SPA",
                                                        "tradeName": "VALIANSPA",
                                                        "activity": "GESTION Y ASESORIAS",
                                                        "address": "AVENIDA CONSISTORIAL 2401 OF 607",
                                                        "commune": "NUNOA",
                                                        "region": "METROPOLINTANA",
                                                        "email": "ICARDENASC@VALIANSPA.COM",
                                                        "phone": "981885606",
                                                        "logoUrl": null,
                                                        "status": "ACTIVE",
                                                        "createdAt": "2025-07-05T09:51:07.088958100Z",
                                                        "updatedAt": "2025-07-05T09:51:07.088958100Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Company with this RUT already exists.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "Company with this RUT already exists.",
                                                    summary = "La compañia con el rut ya existe",
                                                    value = """
                                                              {
                                                                  "timestamp": "2025-07-05T09:51:35.494581600Z",
                                                                  "status": 409,
                                                                  "error": "Conflict",
                                                                  "message": "Company with this RUT already exists.",
                                                                  "path": "/api/v1/companies",
                                                                  "validationErrors": null
                                                              }
                                                            """
                                            ),
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "One or more fields are invalid.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "One or more fields are invalid.",
                                                    summary = "One or more fields are invalid.",
                                                    value = """
                                                            {
                                                                "timestamp": "2025-07-05T09:52:23.394261700Z",
                                                                "status": 400,
                                                                "error": "Validation Failed",
                                                                "message": "One or more fields are invalid.",
                                                                "path": "/api/v1/companies",
                                                                "validationErrors": {
                                                                    "rut": "no debe estar vacío"
                                                                }
                                                            }
                                                            """
                                            ),
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Muchas solicitudes",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "Too Many Request",
                                            summary = "Se realizo muchas solicitudes en un corto periodo de tiempo",
                                            value = """
                                                      {
                                                          "error": "Too Many Requests"
                                                      }
                                                    """
                                    )
                            )
                    ),
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

    @Operation(
            summary = "Actualizar mi empresa (según el contexto del token)",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Company successfully updated",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CompanyResponse.class),
                                    examples = @ExampleObject(
                                            name = "Company data",
                                            summary = "Se obtiene correctamente la informacion actualizada de la empresa",
                                            value = """
                                                    {
                                                        "id": "26823bec-b626-4547-956e-3bcae0352d13",
                                                        "rut": "77180451-9",
                                                        "businessName": "INVERSIONES VALIAN SPA",
                                                        "tradeName": "VALIANSPA",
                                                        "activity": "GESTION Y ASESORIAS",
                                                        "address": "AVENIDA CONSISTORIAL 2401 OF 607",
                                                        "commune": "NUNOA",
                                                        "region": "METROPOLINTANA",
                                                        "email": "ICARDENASC@VALIANSPA.COM",
                                                        "phone": "981885606",
                                                        "logoUrl": null,
                                                        "status": "ACTIVE",
                                                        "createdAt": "2025-07-05T09:51:07.088958100Z",
                                                        "updatedAt": "2025-07-05T09:51:07.088958100Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Company not found.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "Company not found.",
                                                    summary = "La compañia no existe",
                                                    value = """
                                                              {
                                                                  "timestamp": "2025-07-05T09:51:35.494581600Z",
                                                                  "status": 404,
                                                                  "error": "Conflict",
                                                                  "message": "Company not found.",
                                                                  "path": "/api/v1/companies/me",
                                                                  "validationErrors": null
                                                              }
                                                            """
                                            ),
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Muchas solicitudes",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "Too Many Request",
                                            summary = "Se realizo muchas solicitudes en un corto periodo de tiempo",
                                            value = """
                                                      {
                                                          "error": "Too Many Requests"
                                                      }
                                                    """
                                    )
                            )
                    ),
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
            summary = "Endpoint para obtener los datos de la empresa asociada al usuario autenticado.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Company successfully retrieve",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CompanyResponse.class),
                                    examples = @ExampleObject(
                                            name = "Company data",
                                            summary = "Se obtiene correctamente la informacion de la empresa",
                                            value = """
                                                    {
                                                        "id": "26823bec-b626-4547-956e-3bcae0352d13",
                                                        "rut": "77180451-9",
                                                        "businessName": "INVERSIONES VALIAN SPA",
                                                        "tradeName": "VALIANSPA",
                                                        "activity": "GESTION Y ASESORIAS",
                                                        "address": "AVENIDA CONSISTORIAL 2401 OF 607",
                                                        "commune": "NUNOA",
                                                        "region": "METROPOLINTANA",
                                                        "email": "ICARDENASC@VALIANSPA.COM",
                                                        "phone": "981885606",
                                                        "logoUrl": null,
                                                        "status": "ACTIVE",
                                                        "createdAt": "2025-07-05T09:51:07.088958100Z",
                                                        "updatedAt": "2025-07-05T09:51:07.088958100Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Company not found.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "Company not found.",
                                                    summary = "La compañia no existe",
                                                    value = """
                                                              {
                                                                  "timestamp": "2025-07-05T09:51:35.494581600Z",
                                                                  "status": 404,
                                                                  "error": "Conflict",
                                                                  "message": "Company not found.",
                                                                  "path": "/api/v1/companies/me",
                                                                  "validationErrors": null
                                                              }
                                                            """
                                            ),
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Muchas solicitudes",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "Too Many Request",
                                            summary = "Se realizo muchas solicitudes en un corto periodo de tiempo",
                                            value = """
                                                      {
                                                          "error": "Too Many Requests"
                                                      }
                                                    """
                                    )
                            )
                    ),
            }
    )
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','VIEWER')")
    public ResponseEntity<CompanyResponse> getMyCompany() {
        UUID companyId = SecurityUtil.getCompanyIdFromContext();
        return ResponseEntity.ok(companyService.getCompany(companyId));
    }
}
