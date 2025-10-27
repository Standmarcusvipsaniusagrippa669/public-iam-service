package com.valiantech.core.iam.company.service;

import com.valiantech.core.iam.audit.model.AuditAction;
import com.valiantech.core.iam.audit.model.AuditLogEntry;
import com.valiantech.core.iam.audit.model.Metadata;
import com.valiantech.core.iam.audit.model.ResourceType;
import com.valiantech.core.iam.audit.service.UserAuditLogService;
import com.valiantech.core.iam.company.dto.*;
import com.valiantech.core.iam.company.model.Company;
import com.valiantech.core.iam.company.model.CompanyStatus;
import com.valiantech.core.iam.company.repository.CompanyRepository;
import com.valiantech.core.iam.exception.ConflictException;
import com.valiantech.core.iam.exception.NotFoundException;
import com.valiantech.core.iam.security.SecurityUtil;
import com.valiantech.core.iam.user.dto.CreateUserRequest;
import com.valiantech.core.iam.user.dto.UserResponse;
import com.valiantech.core.iam.user.service.UserService;
import com.valiantech.core.iam.usercompany.model.UserCompanyRole;
import com.valiantech.core.iam.usercompany.service.UserCompanyService;
import com.valiantech.core.iam.util.ClientInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;


/**
 * Servicio que gestiona las operaciones relacionadas con las entidades de tipo "Empresa".
 * Proporciona funcionalidades como el proceso de onboarding, la actualización de información
 * de empresas ya existentes y la obtención de detalles de una empresa específica.
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserService userService;
    private final UserCompanyService userCompanyService;
    private final UserAuditLogService userAuditLogService;
    private final ClientInfoService clientInfoService;

    /**
     * Realiza el proceso de onboarding para registrar una nueva empresa y su usuario fundador.
     * Incluye la validación de existencia previa de la empresa, el registro de la empresa,
     * la creación del usuario fundador como activo y validado, la asociación entre la empresa
     * y el usuario fundador, y la auditoría del proceso.
     *
     * @param request Objeto que contiene los datos necesarios para el registro de la empresa
     *                ({@link CreateCompanyRequest}) y del usuario fundador
     *                ({@link CreateUserRequest}).
     * @return Un objeto {@link CompanyResponse} que contiene la información de la empresa registrada.
     * @throws ConflictException Si ya existe una empresa registrada con el mismo RUT.
     */
    @Transactional
    public CompanyResponse onboarding(CompanyOnboardingRequest request) {
        log.debug("Starting onboarding for company with rut={}", request.company().rut());
        if (companyRepository.findByRut(request.company().rut()).isPresent()) {
            log.warn("Company with rut={} already exists", request.company().rut());
            throw new ConflictException("Company with this RUT already exists.");
        }

        Company company = Company.builder()
                .id(UUID.randomUUID())
                .rut(request.company().rut())
                .businessName(request.company().businessName())
                .tradeName(request.company().tradeName())
                .activity(request.company().activity())
                .address(request.company().address())
                .commune(request.company().commune())
                .region(request.company().region())
                .email(request.company().email())
                .phone(request.company().phone())
                .logoUrl(request.company().logoUrl())
                .status(CompanyStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        company = companyRepository.save(company);
        log.debug("Company successfully saved");

        // 2. Crear usuario fundador (activo y validado)
        UserResponse userResponse = userService.registerActiveUser(request.owner());
        log.debug("User owner for company rut={} created", request.company().rut());
        // 3. Crear vínculo OWNER en user_companies
        userCompanyService.registerUserCompany(
                userResponse.id(),
                company.getId(),
                UserCompanyRole.OWNER
        );
        log.debug("User owner associated to company with rut={}", request.company().rut());
        userAuditLogService.logAsync(
                AuditLogEntry.builder()
                        .userId(userResponse.id())
                        .companyId(userResponse.id())
                        .targetUserId(null)
                        .resourceType(ResourceType.COMPANY)
                        .resourceId(company.getId())
                        .action(AuditAction.ONBOARDING)
                        .metadata(new Metadata("Onboarding of company successfully"))
                        .cookies(clientInfoService.getCookies())
                        .ipAddress(clientInfoService.getClientIp())
                        .userAgent(clientInfoService.getUserAgent())
                        .build()
        );
        log.info("Onboarding of company rut {} end successfully", request.company().rut());
        return map(company);
    }

    /**
     * Actualiza los datos de una empresa existente con la información proporcionada en el request.
     *
     * Este método permite modificar campos específicos de una empresa con base en los valores
     * proporcionados en la instancia de {@code UpdateCompanyRequest}. Solo los campos no nulos
     * serán actualizados. Además, registra un log de auditoría para documentar la actualización
     * realizada y persiste los cambios en la base de datos.
     *
     * @param id Identificador único de la empresa (UUID) que se desea actualizar.
     * @param request Objeto que contiene los datos opcionales a actualizar para la empresa.
     *                Cada campo no nulo en el request será aplicado al registro existente.
     * @return Una instancia de {@code CompanyResponse} que representa el estado actualizado
     *         de la empresa después de la operación.
     * @throws NotFoundException Si no se encuentra una empresa con el ID proporcionado.
     */
    public CompanyResponse updateCompany(UUID id, UpdateCompanyRequest request) {
        log.debug("Starting updateCompany for id={}", id);
        Company entity = companyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Company not found."));

        if (request.businessName() != null) entity.setBusinessName(request.businessName());
        if (request.tradeName() != null) entity.setTradeName(request.tradeName());
        if (request.activity() != null) entity.setActivity(request.activity());
        if (request.address() != null) entity.setAddress(request.address());
        if (request.commune() != null) entity.setCommune(request.commune());
        if (request.region() != null) entity.setRegion(request.region());
        if (request.email() != null) entity.setEmail(request.email());
        if (request.phone() != null) entity.setPhone(request.phone());
        if (request.logoUrl() != null) entity.setLogoUrl(request.logoUrl());
        if (request.status() != null) entity.setStatus(request.status());

        entity.setUpdatedAt(Instant.now());

        userAuditLogService.logAsync(
                AuditLogEntry.builder()
                        .userId(SecurityUtil.getUserIdFromContext())
                        .companyId(id)
                        .targetUserId(null)
                        .resourceType(ResourceType.COMPANY)
                        .resourceId(id)
                        .action(AuditAction.COMPANY_UPDATED)
                        .metadata(new Metadata("Successfully update company", request))
                        .cookies(clientInfoService.getCookies())
                        .ipAddress(clientInfoService.getClientIp())
                        .userAgent(clientInfoService.getUserAgent())
                        .build()
        );
        log.info("Successfully update company with id {}", id);
        return map(companyRepository.save(entity));
    }

    /**
     * Obtiene la información de una empresa específica a partir de su identificador único.
     *
     * @param id Identificador único (UUID) de la empresa a consultar.
     * @return Un {@link CompanyResponse} que contiene los datos de la empresa.
     * @throws NotFoundException Si no se encuentra una empresa con el ID proporcionado.
     */
    public CompanyResponse getCompany(UUID id) {
        Company entity = companyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Company not found."));
        return map(entity);
    }

    /**
     * Mapea una entidad {@link Company} a un objeto de transferencia de datos (DTO) {@link CompanyResponse}.
     *
     * @param c la entidad {@code Company} que contiene los datos a mapear.
     * @return un objeto {@code CompanyResponse} con los datos equivalentes extraídos de la entidad {@code Company}.
     */
    private CompanyResponse map(Company c) {
        return new CompanyResponse(
                c.getId(),
                c.getRut(),
                c.getBusinessName(),
                c.getTradeName(),
                c.getActivity(),
                c.getAddress(),
                c.getCommune(),
                c.getRegion(),
                c.getEmail(),
                c.getPhone(),
                c.getLogoUrl(),
                c.getStatus(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
