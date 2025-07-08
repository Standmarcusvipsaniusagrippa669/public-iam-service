package com.valiantech.core.iam.auth.service;

import com.valiantech.core.iam.auth.dto.*;
import com.valiantech.core.iam.auth.model.LoginTicket;
import com.valiantech.core.iam.auth.model.RefreshToken;
import com.valiantech.core.iam.auth.repository.LoginTicketRepository;
import com.valiantech.core.iam.auth.repository.RefreshTokenRepository;
import com.valiantech.core.iam.company.model.Company;
import com.valiantech.core.iam.company.repository.CompanyRepository;
import com.valiantech.core.iam.exception.UnauthorizedException;
import com.valiantech.core.iam.security.SecurityUtil;
import com.valiantech.core.iam.user.dto.UserResponse;
import com.valiantech.core.iam.user.model.User;
import com.valiantech.core.iam.user.model.UserStatus;
import com.valiantech.core.iam.user.repository.UserRepository;
import com.valiantech.core.iam.usercompany.model.UserCompany;
import com.valiantech.core.iam.usercompany.model.UserCompanyStatus;
import com.valiantech.core.iam.usercompany.repository.UserCompanyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
    private final CompanyRepository companyRepository;
    private final LoginTicketRepository loginTicketRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Valida las credenciales y retorna las compañías activas asociadas al usuario, junto con un ticket temporal de login.
     *
     * @param request LoginRequest con email y contraseña del usuario.
     * @return Un objeto con el usuario autenticado, sus empresas y el loginTicket temporal.
     * @throws UnauthorizedException Si las credenciales son incorrectas o el usuario no está activo.
     */
    public AssociatedCompanies fetchCompanies(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        if (!user.getStatus().equals(UserStatus.ACTIVE)) {
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

        List<UserCompany> companies = userCompanyRepository.findByUserId(user.getId());
        List<CompanySummary> summaries = companies.stream()
                .filter(uc -> uc.getStatus().equals(UserCompanyStatus.ACTIVE))
                .map(uc -> {
                    Company company = companyRepository.findById(uc.getCompanyId()).orElse(null);
                    return new CompanySummary(
                            uc.getCompanyId(),
                            company != null ? company.getBusinessName() : "",
                            uc.getRole().name()
                    );
                })
                .toList();

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
        LoginTicket ticket = loginTicketRepository.findById(request.loginTicket())
                .orElseThrow(() -> new UnauthorizedException("Invalid login ticket"));
        log.info("ticket {}", ticket);

        if (ticket.isUsed() || ticket.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Ticket expired or already used");
        }
        if (!ticket.getEmail().equalsIgnoreCase(request.email())) {
            throw new UnauthorizedException("Ticket does not match user");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));
        log.info("user {}", user);

        UserCompany userCompany = userCompanyRepository.findByUserIdAndCompanyId(user.getId(), request.companyId())
                .orElseThrow(() -> new UnauthorizedException("Not affiliated to this company"));
        log.info("userCompany {}", userCompany);

        if (!userCompany.getStatus().equals(UserCompanyStatus.ACTIVE)) {
            throw new UnauthorizedException("Not active in this company");
        }

        String role = userCompany.getRole().name();
        log.info("role {}", role);

        String authToken = jwtService.generateToken(user, request.companyId(), role);
        log.info("authToken {}", authToken);

        // Genera el refresh token como UUID string
        String refreshTokenPlain = UUID.randomUUID().toString();
        log.info("refreshTokenPlain {}", refreshTokenPlain);

        saveRefreshToken(request.companyId(), refreshTokenPlain, user);

        // Invalida el ticket
        ticket.setUsed(true);
        loginTicketRepository.save(ticket);
        log.info("ticket {}", ticket);

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
        String refreshTokenPlain = request.refreshToken();
        String refreshTokenHash = SecurityUtil.sha256Hex(refreshTokenPlain);

        // Buscar refresh token en BD
        RefreshToken tokenEntity = refreshTokenRepository.findByTokenHash(refreshTokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        log.info("tokenEntity {}", tokenEntity);
        // Validar estado y expiración
        if (tokenEntity.isRevoked() || tokenEntity.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }

        // Obtener usuario y empresa para generar nuevo JWT
        User user = userRepository.findById(tokenEntity.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token or user"));
        log.info("user {}", user);

        UserCompany userCompany = userCompanyRepository.findByUserIdAndCompanyId(user.getId(), tokenEntity.getCompanyId())
                .orElseThrow(() -> new UnauthorizedException("Not affiliated to this company"));
        log.info("userCompany {}", userCompany);
        if (!userCompany.getStatus().equals(UserCompanyStatus.ACTIVE)) {
            throw new UnauthorizedException("Not active in this company");
        }

        String role = userCompany.getRole().name();
        log.info("role {}", role);
        // Generar nuevo auth token (JWT)
        String newAuthToken = jwtService.generateToken(user, tokenEntity.getCompanyId(), role);
        log.info("newAuthToken {}", newAuthToken);

        // Rotación de refresh tokens: revocar el antiguo y generar uno nuevo
        tokenEntity.setRevoked(true);
        refreshTokenRepository.save(tokenEntity);
        log.info("tokenEntity {}", tokenEntity);

        String newRefreshTokenPlain = UUID.randomUUID().toString();
        saveRefreshToken(tokenEntity.getCompanyId(), newRefreshTokenPlain, user);

        return new RefreshTokenResponse(newAuthToken, newRefreshTokenPlain);
    }

    /**
     * Guarda un nuevo refresh token en la base de datos.
     * <p>
     * Calcula el hash SHA-256 del token plano para almacenamiento seguro,
     * asigna metadatos como fechas de emisión y expiración, y marca el token como no revocado.
     * </p>
     *
     * @param companyId         el UUID de la empresa asociada al token.
     * @param refreshTokenPlain el token plano generado para el cliente.
     * @param user              el usuario asociado al token.
     */

    private void saveRefreshToken(UUID companyId, String refreshTokenPlain, User user) {
        // Calcula el hash SHA-256 del refresh token para almacenarlo seguro
        String refreshTokenHash = SecurityUtil.sha256Hex(refreshTokenPlain);

        // Crea la entidad RefreshToken para persistirla
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setCompanyId(companyId);
        refreshToken.setTokenHash(refreshTokenHash);
        refreshToken.setIssuedAt(Instant.now());
        refreshToken.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS)); // Expira en 30 días (ajustable)
        refreshToken.setRevoked(false);

        // Guarda el refresh token en la base de datos
        refreshTokenRepository.save(refreshToken);
    }
}
