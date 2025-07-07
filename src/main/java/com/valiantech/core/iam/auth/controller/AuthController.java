package com.valiantech.core.iam.auth.controller;

import com.valiantech.core.iam.auth.dto.LoginRequest;
import com.valiantech.core.iam.auth.dto.AssociatedCompanies;
import com.valiantech.core.iam.auth.dto.LoginResponse;
import com.valiantech.core.iam.auth.dto.TokenRequest;
import com.valiantech.core.iam.auth.service.AuthService;
import com.valiantech.core.iam.ratelimit.RateLimit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @PostMapping("/login-with-company")
    @RateLimit(capacity = 5, refill = 10)
    public ResponseEntity<LoginResponse> loginWithCompany(@RequestBody TokenRequest request) {
        return ResponseEntity.ok(authService.loginWithCompany(request));
    }
}
