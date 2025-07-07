package com.valiantech.core.iam.company.dto;

import com.valiantech.core.iam.company.model.CompanyStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de respuesta para los endpoints relacionados a empresas (compañías).
 *
 * <p>
 * Representa los datos públicos y administrativos de una empresa,
 * usados en las respuestas de onboarding, consulta y actualización.
 * </p>
 *
 * <b>Campos:</b>
 * <ul>
 *   <li><b>id:</b> Identificador único de la empresa (UUID).</li>
 *   <li><b>rut:</b> RUT chileno de la empresa.</li>
 *   <li><b>businessName:</b> Razón social registrada en el SII.</li>
 *   <li><b>tradeName:</b> Nombre de fantasía o comercial.</li>
 *   <li><b>activity:</b> Giro o actividad económica.</li>
 *   <li><b>address:</b> Dirección principal.</li>
 *   <li><b>commune:</b> Comuna.</li>
 *   <li><b>region:</b> Región administrativa.</li>
 *   <li><b>email:</b> Correo de contacto principal.</li>
 *   <li><b>phone:</b> Teléfono de contacto.</li>
 *   <li><b>logoUrl:</b> URL del logo de la empresa (opcional).</li>
 *   <li><b>status:</b> Estado actual de la empresa ({@link CompanyStatus}).</li>
 *   <li><b>createdAt:</b> Fecha de creación.</li>
 *   <li><b>updatedAt:</b> Fecha de última actualización.</li>
 * </ul>
 *
 * @param id         Identificador único de la empresa.
 * @param rut        RUT de la empresa.
 * @param businessName Razón social registrada.
 * @param tradeName  Nombre de fantasía o comercial.
 * @param activity   Giro o actividad económica.
 * @param address    Dirección principal.
 * @param commune    Comuna.
 * @param region     Región administrativa.
 * @param email      Correo de contacto principal.
 * @param phone      Teléfono de contacto.
 * @param logoUrl    URL del logo (puede ser nulo).
 * @param status     Estado actual de la empresa.
 * @param createdAt  Fecha de creación.
 * @param updatedAt  Fecha de última actualización.
 */
public record CompanyResponse(
        UUID id,
        String rut,
        String businessName,
        String tradeName,
        String activity,
        String address,
        String commune,
        String region,
        String email,
        String phone,
        String logoUrl,
        CompanyStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
