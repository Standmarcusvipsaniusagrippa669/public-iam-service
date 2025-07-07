package com.valiantech.core.iam.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request para la creación de una nueva empresa (compañía).
 *
 * <p>
 * Este DTO es utilizado al realizar el onboarding de una empresa o al crearla desde un flujo administrativo.
 * Incluye los datos requeridos y opcionales para el registro, así como anotaciones de validación
 * para garantizar la integridad mínima de los datos ingresados.
 * </p>
 *
 * <b>Campos:</b>
 * <ul>
 *   <li><b>rut:</b> RUT chileno de la empresa (máx. 10 caracteres). <br>
 *       <i>Validaciones:</i> {@code @NotBlank}, {@code @Size(max = 10)}</li>
 *   <li><b>businessName:</b> Razón social registrada. <br>
 *       <i>Validaciones:</i> {@code @NotBlank}</li>
 *   <li><b>tradeName:</b> Nombre de fantasía o comercial (opcional).</li>
 *   <li><b>activity:</b> Giro o actividad económica (opcional).</li>
 *   <li><b>address:</b> Dirección principal (opcional).</li>
 *   <li><b>commune:</b> Comuna (opcional).</li>
 *   <li><b>region:</b> Región administrativa (opcional).</li>
 *   <li><b>email:</b> Correo de contacto principal (opcional).</li>
 *   <li><b>phone:</b> Teléfono de contacto (opcional).</li>
 *   <li><b>logoUrl:</b> URL del logo de la empresa (opcional).</li>
 * </ul>
 *
 * <h3>Ejemplo de uso:</h3>
 * <pre>
 * {
 *   "rut": "12345678-9",
 *   "businessName": "Mi Empresa S.A.",
 *   "tradeName": "Comercial XYZ",
 *   "activity": "Servicios TI",
 *   "address": "Av. Principal 123",
 *   "commune": "Santiago",
 *   "region": "Metropolitana",
 *   "email": "contacto@empresa.com",
 *   "phone": "+56 9 1234 5678",
 *   "logoUrl": "https://miempresa.com/logo.png"
 * }
 * </pre>
 *
 * @param rut          RUT chileno de la empresa.
 * @param businessName Razón social registrada.
 * @param tradeName    Nombre de fantasía o comercial.
 * @param activity     Giro o actividad económica.
 * @param address      Dirección principal.
 * @param commune      Comuna.
 * @param region       Región administrativa.
 * @param email        Correo de contacto principal.
 * @param phone        Teléfono de contacto.
 * @param logoUrl      URL del logo de la empresa.
 */
public record CreateCompanyRequest(
        @NotBlank @Size(max = 10)
        String rut,

        @NotBlank
        String businessName,

        String tradeName,
        String activity,
        String address,
        String commune,
        String region,
        String email,
        String phone,
        String logoUrl
) {}
