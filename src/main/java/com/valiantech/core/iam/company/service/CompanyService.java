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
 * Servicio para la gestión de empresas (compañías) en el sistema IAM.
 *
 * <p>
 * Implementa la lógica de negocio para el proceso de onboarding (registro), actualización y obtención de datos
 * de una compañía. Se encarga de validar unicidad de RUT, coordinar la creación de usuarios founder (owner)
 * y establecer la relación OWNER en la tabla de vínculo usuario-empresa.
 * </p>
 *
 * <h3>Responsabilidades principales:</h3>
 * <ul>
 *   <li>Registrar una nueva empresa y su usuario owner mediante onboarding.</li>
 *   <li>Actualizar los datos de la empresa a partir de un request de modificación.</li>
 *   <li>Obtener la información detallada de una empresa por ID.</li>
 *   <li>Validar unicidad de RUT para evitar duplicidades.</li>
 *   <li>Coordinar la creación y vinculación de usuario founder y roles iniciales.</li>
 * </ul>
 *
 * <b>Notas:</b>
 * <ul>
 *   <li>El onboarding es transaccional: se crean la empresa, el usuario owner y su vínculo OWNER atómicamente.</li>
 *   <li>El método {@link #updateCompany(UUID, UpdateCompanyRequest)} solo actualiza campos no nulos en el request.</li>
 *   <li>Se lanza {@link ConflictException} si el RUT ya existe, y {@link NotFoundException} si no se encuentra la empresa.</li>
 * </ul>
 *
 * @author Ian Cardenas
 * @since 1.0
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
     * Registra una nueva empresa y su usuario owner (onboarding).
     * Crea la compañía, el usuario founder y la relación OWNER en user_companies de manera atómica.
     *
     * @param request Request con datos de la empresa y del usuario owner.
     * @return Datos de la empresa registrada.
     * @throws ConflictException Si ya existe una empresa con el mismo RUT.
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
     * Actualiza los datos de la empresa indicada, modificando solo los campos no nulos del request.
     *
     * @param id      ID de la empresa a actualizar.
     * @param request Request con los campos a modificar.
     * @return Empresa actualizada.
     * @throws NotFoundException Si la empresa no existe.
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
     * Obtiene los datos detallados de una empresa por su ID.
     *
     * @param id ID de la empresa.
     * @return Datos de la empresa.
     * @throws NotFoundException Si la empresa no existe.
     */
    public CompanyResponse getCompany(UUID id) {
        Company entity = companyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Company not found."));
        return map(entity);
    }

    /**
     * Transforma la entidad Company a DTO de respuesta.
     *
     * @param c Entidad Company.
     * @return DTO de respuesta para la empresa.
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
