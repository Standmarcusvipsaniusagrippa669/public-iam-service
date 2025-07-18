package com.valiantech.core.iam.auth.service;

import com.valiantech.core.iam.audit.model.AuditAction;
import com.valiantech.core.iam.audit.model.AuditLogEntry;
import com.valiantech.core.iam.audit.model.Metadata;
import com.valiantech.core.iam.audit.model.ResourceType;
import com.valiantech.core.iam.audit.service.UserAuditLogService;
import com.valiantech.core.iam.auth.dto.*;
import com.valiantech.core.iam.auth.model.LoginTicket;
import com.valiantech.core.iam.auth.model.RefreshToken;
import com.valiantech.core.iam.auth.repository.LoginTicketRepository;
import com.valiantech.core.iam.clients.CompanyClient;
import com.valiantech.core.iam.company.dto.CompanyResponse;
import com.valiantech.core.iam.exception.UnauthorizedException;
import com.valiantech.core.iam.security.SecurityUtil;
import com.valiantech.core.iam.user.dto.UserResponse;
import com.valiantech.core.iam.user.model.User;
import com.valiantech.core.iam.user.model.UserStatus;
import com.valiantech.core.iam.user.repository.UserRepository;
import com.valiantech.core.iam.usercompany.model.UserCompany;
import com.valiantech.core.iam.usercompany.model.UserCompanyStatus;
import com.valiantech.core.iam.usercompany.repository.UserCompanyRepository;
import com.valiantech.core.iam.util.ClientInfoService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.time.temporal.ChronoUnit;

/**
 * Servicio encargado de la autenticación de usuarios y el manejo del flujo de login multiempresa.
 *
 * <p>
 * Gestiona la validación de credenciales, la obtención de compañías asociadas a un usuario,
 * la emisión de tickets temporales para login seguro y la generación de JWTs con contexto de empresa y rol.
 * Implementa el flujo de autenticación en dos pasos para ambientes multi-tenant.
 * </p>
 *
 * <h3>Responsabilidades principales:</h3>
 * <ul>
 *     <li>Validar credenciales y estado del usuario.</li>
 *     <li>Emitir un ticket de login de un solo uso y expiración corta.</li>
 *     <li>Listar empresas asociadas al usuario, junto con el rol en cada una.</li>
 *     <li>Verificar el ticket y credenciales para emitir el JWT final.</li>
 *     <li>Prevenir ataques de fuerza bruta y asegurar el flujo seguro de login.</li>
 * </ul>
 *
 * <b>Notas:</b>
 * <ul>
 *     <li>El ticket de login solo puede usarse una vez y expira en 5 minutos.</li>
 *     <li>El JWT generado incluye el companyId y rol actual para operaciones multiempresa seguras.</li>
 *     <li>Lanza {@link UnauthorizedException} para todos los errores de autenticación y acceso.</li>
 * </ul>
 *
 * @author Ian Cardenas
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid credentials";
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserCompanyRepository userCompanyRepository;
    private final CompanyClient companyClient;
    private final LoginTicketRepository loginTicketRepository;
    private final UserAuditLogService userAuditLogService;
    private final ClientInfoService clientInfoService;
    private final UserLoginService userLoginService;
    private final RefreshTokenService refreshTokenService;

    /**
     * Valida las credenciales y retorna las compañías activas asociadas al usuario, junto con un ticket temporal de login.
     *
     * @param request LoginRequest con email y contraseña del usuario.
     * @return Un objeto con el usuario autenticado, sus empresas y el loginTicket temporal.
     * @throws UnauthorizedException Si las credenciales son incorrectas o el usuario no está activo.
     */
    public AssociatedCompanies fetchCompanies(LoginRequest request) {
        log.debug("Starting fetchCompanies with email={}", request.email());
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));
        log.debug("User found with email={}", request.email());

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            userAuditLogService.logAsync(
                    AuditLogEntry.builder()
                            .userId(user.getId())
                            .companyId(null)
                            .targetUserId(user.getId())
                            .resourceType(ResourceType.USER)
                            .resourceId(user.getId())
                            .action(AuditAction.LOGIN_FAILURE)
                            .metadata(new Metadata("User password not match."))
                            .cookies(clientInfoService.getCookies())
                            .ipAddress(clientInfoService.getClientIp())
                            .userAgent(clientInfoService.getUserAgent())
                            .build()
            );
            userLoginService.recordLoginAttempt(
                    user.getId(),
                    null,
                    clientInfoService.getClientIp(),
                    clientInfoService.getUserAgent(),
                    false,
                    "Password not match"
            );
            log.warn("User password not match for email={}", request.email());
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        if (!user.getStatus().equals(UserStatus.ACTIVE)) {
            log.warn("User not active for email={}", request.email());
            throw new UnauthorizedException("User is not active");
        }

        // Generar loginTicket temporal
        String loginTicket = UUID.randomUUID().toString();
        LoginTicket ticket = LoginTicket.builder()
                .id(loginTicket)
                .email(user.getEmail())
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .used(false)
                .build();
        loginTicketRepository.save(ticket);
        log.debug("Login ticket saved for email={}", request.email());

        List<UserCompany> companies = userCompanyRepository.findByUserId(user.getId());
        log.debug("Companies found for email={}: {}", request.email(), companies.size());
        String token = jwtService.generateServiceTokenWithIdentifications(
                user.getId(),
                null,
                "company:read",
                Duration.ofMinutes(1)
        );
        List<CompanySummary> summaries = companies.stream()
                .filter(uc -> uc.getStatus().equals(UserCompanyStatus.ACTIVE))
                .map(uc -> {
                    CompanyResponse company = companyClient.findById(uc.getCompanyId(), token);
                    return new CompanySummary(
                            uc.getCompanyId(),
                            company != null ? company.businessName(): "",
                            uc.getRole().name()
                    );
                })
                .toList();
        log.info("Login successful for email {}", request.email());
        return new AssociatedCompanies(UserResponse.from(user), summaries, loginTicket);
    }

    /**
     * Realiza el login en el contexto de una empresa específica, validando el ticket y emitiendo el JWT de acceso.
     *
     * @param request TokenRequest con loginTicket, email y companyId seleccionados.
     * @return Respuesta con el JWT de acceso, datos del usuario, empresa y rol.
     * @throws UnauthorizedException Si el ticket es inválido, expirado, ya usado o la afiliación no es válida.
     */
    @Transactional
    public LoginResponse loginWithCompany(TokenRequest request) {
        log.debug("Starting loginWithCompany with email={} and companyId={}", request.email(), request.companyId());
        LoginTicket ticket = loginTicketRepository.findById(request.loginTicket())
                .orElseThrow(() -> new UnauthorizedException("Invalid login ticket"));
        log.debug("Login ticket found with used={} and expiresAt={}", ticket.isUsed(), ticket.getExpiresAt());

        if (ticket.isUsed() || ticket.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Login ticket expired or already used: {}", request.loginTicket());
            throw new UnauthorizedException("Ticket expired or already used");
        }
        if (!ticket.getEmail().equalsIgnoreCase(request.email())) {
            log.warn("Login ticket does not match user: {}", request.loginTicket());
            throw new UnauthorizedException("Ticket does not match user");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));
        log.debug("User found for email={}", request.email());

        UserCompany userCompany = userCompanyRepository.findByUserIdAndCompanyId(user.getId(), request.companyId())
                .orElseThrow(() -> new UnauthorizedException("Not affiliated to this company"));
        log.debug("User found for email={} and company={}", request.email(), request.companyId());

        if (!userCompany.getStatus().equals(UserCompanyStatus.ACTIVE)) {
            log.warn("User not active in this company: {}", request.email());
            throw new UnauthorizedException("Not active in this company");
        }

        String role = userCompany.getRole().name();

        String authToken = jwtService.generateToken(user, request.companyId(), role);
        log.debug("Auth token generate for email={}", request.email());

        // Genera el refresh token como UUID string
        String refreshTokenPlain = refreshTokenService.saveNewRefreshToken(request.companyId(), user);
        log.debug("Refresh token generate for email={}", request.email());

        // Invalida el ticket
        ticket.setUsed(true);
        loginTicketRepository.save(ticket);
        log.debug("Login ticket set used for email={}", request.email());

        userAuditLogService.logAsync(
                AuditLogEntry.builder()
                        .userId(user.getId())
                        .companyId(userCompany.getCompanyId())
                        .targetUserId(user.getId())
                        .resourceType(ResourceType.USER)
                        .resourceId(user.getId())
                        .action(AuditAction.LOGIN_SUCCESS)
                        .metadata(new Metadata("Login successful"))
                        .cookies(clientInfoService.getCookies())
                        .ipAddress(clientInfoService.getClientIp())
                        .userAgent(clientInfoService.getUserAgent())
                        .build()
        );

        userLoginService.recordLoginAttempt(
                user.getId(),
                userCompany.getCompanyId(),
                clientInfoService.getClientIp(),
                clientInfoService.getUserAgent(),
                true,
                null
        );
        log.info("Login successful for user {} in company {}", request.email(), request.companyId());
        return new LoginResponse(authToken, refreshTokenPlain, UserResponse.from(user), request.companyId(), role);
    }

    /**
     * Refresca el token de autenticación (JWT) usando un refresh token válido.
     * <p>
     * Este método valida que el refresh token proporcionado no esté revocado ni expirado,
     * verifica la existencia del usuario y su afiliación activa a la empresa correspondiente,
     * luego genera un nuevo JWT y un nuevo refresh token, revocando el anterior (rotación).
     * </p>
     *
     * @param request objeto que contiene el refresh token plano enviado por el cliente.
     * @return un {@link RefreshTokenResponse} que incluye el nuevo auth token (JWT) y el nuevo refresh token.
     * @throws UnauthorizedException si el refresh token es inválido, revocado o expirado,
     *                               si el usuario no existe, no está afiliado o no está activo en la empresa.
     */
    @Transactional
    public RefreshTokenResponse refreshAuthToken(RefreshTokenRequest request) {
        log.debug("Starting refreshAuthToken for refreshToken={}", request.refreshToken());
        String refreshTokenPlain = request.refreshToken();
        String refreshTokenHash = SecurityUtil.sha256Hex(refreshTokenPlain);

        // Buscar refresh token en BD
        RefreshToken tokenEntity = refreshTokenService.findByTokenHash(refreshTokenHash);
        log.debug("Refresh token found for refreshToken={}", request.refreshToken());

        // Validar estado y expiración
        if (tokenEntity.isRevoked() || tokenEntity.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Refresh token expired or revoked for refreshToken={}", request.refreshToken());
            throw new UnauthorizedException("Refresh token expired or revoked");
        }

        // Obtener usuario y empresa para generar nuevo JWT
        User user = userRepository.findById(tokenEntity.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token or user"));
        log.debug("User found for refreshToken={}", request.refreshToken());

        UserCompany userCompany = userCompanyRepository.findByUserIdAndCompanyId(user.getId(), tokenEntity.getCompanyId())
                .orElseThrow(() -> new UnauthorizedException("Not affiliated to this company"));
        log.debug("User found in company");
        if (!userCompany.getStatus().equals(UserCompanyStatus.ACTIVE)) {
            log.warn("User not active in this company for refreshToken={}", request.refreshToken());
            throw new UnauthorizedException("Not active in this company");
        }

        String role = userCompany.getRole().name();
        // Generar nuevo auth token (JWT)
        String newAuthToken = jwtService.generateToken(user, tokenEntity.getCompanyId(), role);
        log.debug("New auth token generate for refreshToken={}", request.refreshToken());

        // Rotación de refresh tokens: revocar el antiguo y generar uno nuevo
        tokenEntity.setRevoked(true);
        refreshTokenService.updateRefreshToken(tokenEntity);
        log.debug("Revoked previously refresh token for refreshToken={}", request.refreshToken());


        String newRefreshTokenPlain = refreshTokenService.saveNewRefreshToken(tokenEntity.getCompanyId(), user);

        userAuditLogService.logAsync(
                AuditLogEntry.builder()
                        .userId(user.getId())
                        .companyId(tokenEntity.getCompanyId())
                        .targetUserId(user.getId())
                        .resourceType(ResourceType.USER)
                        .resourceId(user.getId())
                        .action(AuditAction.REFRESH_TOKEN_ISSUED)
                        .metadata(new Metadata("Success refresh token."))
                        .cookies(clientInfoService.getCookies())
                        .ipAddress(clientInfoService.getClientIp())
                        .userAgent(clientInfoService.getUserAgent())
                        .build()
        );
        log.info("Success refresh token for refreshToken {}", request.refreshToken());
        return new RefreshTokenResponse(newAuthToken, newRefreshTokenPlain);
    }

    /**
     * Realiza el cierre de sesión (logout) revocando el refresh token proporcionado.
     * <p>
     * Este método valida que el refresh token recibido sea válido, no esté expirado ni revocado,
     * y lo marca como revocado para evitar su uso futuro en la generación de nuevos tokens.
     * </p>
     * <p>
     * Registra logs en diferentes niveles para facilitar la trazabilidad y auditoría del proceso.
     * </p>
     *
     * @param request objeto que contiene el refresh token plano enviado por el cliente para revocación.
     * @return un {@link LogoutResponse} con mensaje de confirmación del cierre de sesión.
     * @throws UnauthorizedException si el refresh token es inválido, ya revocado o expirado.
     */
    @Transactional
    public LogoutResponse logout(LogoutRequest request) {
        log.debug("Starting logout for refreshToken={}", request.refreshToken());
        String refreshTokenPlain = request.refreshToken();
        String refreshTokenHash = SecurityUtil.sha256Hex(refreshTokenPlain);

        RefreshToken tokenEntity = refreshTokenService.findByTokenHash(refreshTokenHash);
        log.debug("Refresh token fetch for refreshToken={}", request.refreshToken());
        if (tokenEntity.isRevoked() || tokenEntity.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Refresh token expired or revoked for refreshToken={}", request.refreshToken());
            throw new UnauthorizedException("Refresh token expired or revoked");
        }

        tokenEntity.setRevoked(true);
        refreshTokenService.updateRefreshToken(tokenEntity);

        userAuditLogService.logAsync(
                AuditLogEntry.builder()
                        .userId(tokenEntity.getUserId())
                        .companyId(tokenEntity.getCompanyId())
                        .targetUserId(tokenEntity.getUserId())
                        .resourceType(ResourceType.USER)
                        .resourceId(tokenEntity.getId())
                        .action(AuditAction.LOGOUT)
                        .metadata(new Metadata("Logout successful"))
                        .cookies(clientInfoService.getCookies())
                        .ipAddress(clientInfoService.getClientIp())
                        .userAgent(clientInfoService.getUserAgent())
                        .build()
        );
        log.info("Revoked refresh token successfully");
        return new LogoutResponse("Logout successful");
    }

    /**
     * Cambia la contraseña de un usuario validando la contraseña actual y aplicando políticas de seguridad.
     * <p>
     * Valida que el usuario exista y que la contraseña actual coincida con la almacenada.
     * Luego codifica la nueva contraseña y actualiza el registro en base de datos.
     * Finalmente, revoca todos los refresh tokens activos para ese usuario para requerir un nuevo login.
     * </p>
     *
     * @param userId  UUID del usuario cuyo password se desea cambiar.
     * @param request objeto con la contraseña actual y la nueva contraseña.
     * @return un {@link ChangePasswordResponse} confirmando que el cambio fue exitoso y que se requiere nuevo login.
     * @throws UnauthorizedException si el usuario no existe o la contraseña actual es incorrecta.
     */
    @Transactional
    public ChangePasswordResponse changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            userAuditLogService.logAsync(
                    AuditLogEntry.builder()
                            .userId(userId)
                            .companyId(null)
                            .targetUserId(userId)
                            .resourceType(ResourceType.USER)
                            .resourceId(userId)
                            .action(AuditAction.ATTEMPT_CHANGE_PASSWORD)
                            .metadata(new Metadata("Current password is incorrect"))
                            .cookies(clientInfoService.getCookies())
                            .ipAddress(clientInfoService.getClientIp())
                            .userAgent(clientInfoService.getUserAgent())
                            .build()
            );
            throw new UnauthorizedException("Current password is incorrect");
        }

        // Aquí validar reglas de seguridad para nueva contraseña (longitud, complejidad...)

        String newPasswordHash = passwordEncoder.encode(request.newPassword());
        user.setPasswordHash(newPasswordHash);
        userRepository.save(user);

        refreshTokenService.revokeAllByUserId(userId);

        userAuditLogService.logAsync(
                AuditLogEntry.builder()
                        .userId(userId)
                        .companyId(null)
                        .targetUserId(userId)
                        .resourceType(ResourceType.USER)
                        .resourceId(userId)
                        .action(AuditAction.PASSWORD_CHANGE)
                        .metadata(new Metadata("Password changed successfully"))
                        .cookies(clientInfoService.getCookies())
                        .ipAddress(clientInfoService.getClientIp())
                        .userAgent(clientInfoService.getUserAgent())
                        .build()
        );

        return new ChangePasswordResponse("Password changed successfully. New login required.");
    }
}
