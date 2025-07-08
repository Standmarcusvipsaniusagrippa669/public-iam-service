package com.valiantech.core.iam.auth.controller;

import com.valiantech.core.iam.auth.dto.*;
import com.valiantech.core.iam.auth.service.AuthService;
import com.valiantech.core.iam.exception.ErrorResponse;
import com.valiantech.core.iam.ratelimit.RateLimit;
import com.valiantech.core.iam.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Controlador REST encargado de la autenticación de usuarios y la obtención de empresas asociadas.
 *
 * <p>
 * Este controlador gestiona el flujo de login en dos pasos:
 * <ol>
 *     <li>El usuario se autentica con su email y contraseña para obtener la lista de empresas a las que pertenece.</li>
 *     <li>Selecciona una empresa y, mediante un ticket de login, obtiene el JWT de acceso para operar en el contexto de esa empresa.</li>
 * </ol>
 * </p>
 *
 * <h3>Endpoints:</h3>
 * <ul>
 *   <li>
 *     <b>POST /api/v1/auth/login</b><br>
 *     <b>Descripción:</b> Valida las credenciales de usuario y retorna las empresas asociadas.<br>
 *     <b>Rate limiting:</b> Máximo 5 intentos por ventana de refill.<br>
 *   </li>
 *   <li>
 *     <b>POST /api/v1/auth/login-with-company</b><br>
 *     <b>Descripción:</b> Permite seleccionar una empresa (usando un ticket válido) y retorna el JWT de acceso.<br>
 *     <b>Rate limiting:</b> Máximo 5 intentos por ventana de refill.<br>
 *   </li>
 * </ul>
 *
 * <p>
 * Ambos endpoints están protegidos por la anotación {@link RateLimit}, que limita la cantidad de intentos para evitar ataques de fuerza bruta.
 * </p>
 *
 * <b>Notas:</b>
 * <ul>
 *   <li>La lógica de negocio está delegada en el servicio {@link AuthService}.</li>
 *   <li>El flujo de autenticación está pensado para ambientes multiempresa/tenant.</li>
 *   <li>El ticket devuelto por {@code /login} expira en pocos minutos y sólo puede usarse una vez.</li>
 * </ul>
 *
 * @author Ian Cardenas
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Endpoint de inicio de sesión: valida credenciales y retorna las empresas asociadas al usuario.
     *
     * @param request Objeto con email y contraseña.
     * @return Lista de empresas asociadas al usuario y ticket de login temporal.
     */
    @Operation(
            summary = "Valida credenciales y retorna las empresas asociadas al usuario",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Validación de credenciales correctas",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AssociatedCompanies.class),
                                    examples = @ExampleObject(
                                            name = "Validado",
                                            summary = "Validación exitosa de credenciales",
                                            value = """
                                                    {
                                                        "user": {
                                                            "id": "ca52f941-ad0e-443f-986c-1b880b67d9f4",
                                                            "fullName": "IAN LUCIANO CARDENAS CASTILLO",
                                                            "email": "icardenasc@valianspa.com",
                                                            "emailValidated": true,
                                                            "status": "ACTIVE",
                                                            "lastLoginAt": null,
                                                            "createdAt": "2025-07-06T21:20:10.457855Z",
                                                            "updatedAt": "2025-07-06T21:20:10.457859Z"
                                                        },
                                                        "companies": [
                                                            {
                                                                "companyId": "1de85aa3-013b-4df6-89e0-9f904c210786",
                                                                "companyName": "INVERSIONES VALIAN SPA",
                                                                "role": "OWNER"
                                                            }
                                                        ],
                                                        "loginTicket": "45235ea3-b1da-4075-b2d7-e365b9a8b860"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Validación de credenciales incorrectas",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "Correo invalido",
                                                    summary = "Correo invalido",
                                                    value = """
                                                      {
                                                           "timestamp": "2025-07-08T03:25:11.460386425Z",
                                                           "status": 401,
                                                           "error": "Unauthorized",
                                                           "message": "Invalid credentials",
                                                           "path": "/api/v1/auth/login",
                                                           "validationErrors": null
                                                       }
                                                    """
                                            ),
                                            @ExampleObject(
                                                    name = "Email invalido",
                                                    summary = "Email invalido",
                                                    value = """
                                                      {
                                                           "timestamp": "2025-07-08T03:25:11.460386425Z",
                                                           "status": 401,
                                                           "error": "Unauthorized",
                                                           "message": "Invalid credentials",
                                                           "path": "/api/v1/auth/login",
                                                           "validationErrors": null
                                                       }
                                                    """
                                            ),
                                            @ExampleObject(
                                                    name = "Usuario descativado",
                                                    summary = "Usuario desactivado",
                                                    value = """
                                                      {
                                                           "timestamp": "2025-07-08T03:25:11.460386425Z",
                                                           "status": 401,
                                                           "error": "Unauthorized",
                                                           "message": "Invalid credentials",
                                                           "path": "/api/v1/auth/login",
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
    @PostMapping("/login")
    @RateLimit(capacity = 5, refill = 10)
    public ResponseEntity<AssociatedCompanies> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.fetchCompanies(request));
    }

    /**
     * Endpoint para finalizar el login seleccionando la empresa y obtener el JWT de acceso.
     *
     * @param request Objeto con ticket de login y empresa seleccionada.
     * @return JWT de acceso y datos de sesión.
     */
    @Operation(
            summary = "Finaliza el login seleccionando la empresa y obtener el JWT de acceso",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Obtención del token",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = LoginResponse.class),
                                    examples = @ExampleObject(
                                            name = "Token",
                                            summary = "Se obtiene correctamente el token",
                                            value = """
                                                    {
                                                         "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjYTUyZjk0MS1hZDBlLTQ0M2YtOTg2Yy0xYjg4MGI2N2Q5ZjQiLCJlbWFpbCI6ImljYXJkZW5hc2NAdmFsaWFuc3BhLmNvbSIsImNvbXBhbnlJZCI6IjFkZTg1YWEzLTAxM2ItNGRmNi04OWUwLTlmOTA0YzIxMDc4NiIsInJvbGUiOiJPV05FUiIsImlhdCI6MTc1MTk0NjQ1MSwiZXhwIjoxNzUyMDMyODUxfQ.LOu4--Nm_G211zIRnomZPF8nqcRE5h1l0E2qaIyMVQo",
                                                         "user": {
                                                             "id": "ca52f941-ad0e-443f-986c-1b880b67d9f4",
                                                             "fullName": "IAN LUCIANO CARDENAS CASTILLO",
                                                             "email": "icardenasc@valianspa.com",
                                                             "emailValidated": true,
                                                             "status": "ACTIVE",
                                                             "lastLoginAt": null,
                                                             "createdAt": "2025-07-06T21:20:10.457855Z",
                                                             "updatedAt": "2025-07-06T21:20:10.457859Z"
                                                         },
                                                         "companyId": "1de85aa3-013b-4df6-89e0-9f904c210786",
                                                         "role": "OWNER"
                                                     }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Validación de credenciales incorrectas",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "Invalid login ticket",
                                                    summary = "El Ticket del /login es invalido",
                                                    value = """
                                                      {
                                                           "timestamp": "2025-07-08T03:25:11.460386425Z",
                                                           "status": 401,
                                                           "error": "Unauthorized",
                                                           "message": "Invalid login ticket",
                                                           "path": "/api/v1/auth//login-with-company",
                                                           "validationErrors": null
                                                       }
                                                    """
                                            ),
                                            @ExampleObject(
                                                    name = "Ticket expired or already used",
                                                    summary = "El Ticket ya expiro o fue usado",
                                                    value = """
                                                      {
                                                           "timestamp": "2025-07-08T03:25:11.460386425Z",
                                                           "status": 401,
                                                           "error": "Unauthorized",
                                                           "message": "Ticket expired or already used",
                                                           "path": "/api/v1/auth//login-with-company",
                                                           "validationErrors": null
                                                       }
                                                    """
                                            ),
                                            @ExampleObject(
                                                    name = "Ticket does not match user",
                                                    summary = "El ticket es de otro usuario",
                                                    value = """
                                                      {
                                                           "timestamp": "2025-07-08T03:25:11.460386425Z",
                                                           "status": 401,
                                                           "error": "Unauthorized",
                                                           "message": "Ticket does not match user",
                                                           "path": "/api/v1/auth//login-with-company",
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
    @PostMapping("/login-with-company")
    @RateLimit(capacity = 5, refill = 10)
    public ResponseEntity<LoginResponse> loginWithCompany(@RequestBody TokenRequest request) {
        return ResponseEntity.ok(authService.loginWithCompany(request));
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Obtener los datos del usuario autenticado y el contexto actual",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Sesión actual",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "Ejemplo whoami",
                                            value = """
                                                    {
                                                        "userId": "32b01416-4c3b-4ad2-9f99-49c4cf68cf3c",
                                                        "fullName": "Juan Pérez",
                                                        "email": "juan.perez@empresa.com",
                                                        "emailValidated": true,
                                                        "status": "ACTIVE",
                                                        "companyId": "caf50e5e-885a-4a2b-8c4a-c51e43a6711a",
                                                        "companyName": "Empresa S.A.",
                                                        "role": "ADMIN"
                                                    }
                                                    """
                                    )
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
    @GetMapping("/whoami")
    public ResponseEntity<WhoamiResponse> whoami() {
        // 1. Extraer info desde el contexto de seguridad/JWT
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var details = (Map<String, Object>) auth.getDetails();

        UUID userId = ((User) auth.getPrincipal()).getId();
        String email = (String) details.get("email");
        String fullName = (String) details.get("fullName");
        boolean emailValidated = (boolean) details.getOrDefault("emailValidated", false);
        String status = (String) details.getOrDefault("status", "unknown");
        UUID companyId = UUID.fromString((String) details.get("companyId"));
        String companyName = (String) details.getOrDefault("companyName", "unknown");
        String role = (String) details.get("role");

        return ResponseEntity.ok(new WhoamiResponse(
                userId,
                fullName,
                email,
                emailValidated,
                status,
                companyId,
                companyName,
                role
        ));
    }
}
