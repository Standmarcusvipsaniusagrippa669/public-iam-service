package com.valiantech.core.iam.company.dto;

import com.valiantech.core.iam.user.dto.CreateUserRequest;

/**
 * Request para el proceso de onboarding (registro inicial) de una nueva empresa y su usuario fundador.
 *
 * <p>
 * Contiene los datos necesarios para crear la empresa y el usuario owner (fundador),
 * permitiendo que ambos se creen en un solo flujo atómico durante el onboarding.
 * </p>
 *
 * <b>Campos:</b>
 * <ul>
 *   <li><b>company:</b> Datos de la nueva empresa a registrar ({@link CreateCompanyRequest}).</li>
 *   <li><b>owner:</b> Datos del usuario que será el fundador y tendrá el rol OWNER ({@link CreateUserRequest}).</li>
 * </ul>
 *
 * <h3>Ejemplo de uso:</h3>
 * <pre>
 * POST /api/v1/companies/onboarding
 * {
 *   "company": {
 *     "rut": "12345678-9",
 *     "businessName": "Mi Empresa S.A.",
 *     ...
 *   },
 *   "owner": {
 *     "fullName": "Juan Pérez",
 *     "email": "juan@empresa.com",
 *     "password": "secreto123"
 *   }
 * }
 * </pre>
 *
 * @param company Datos de la empresa.
 * @param owner   Datos del usuario owner (fundador).
 */
public record CompanyOnboardingRequest(
        CreateCompanyRequest company,
        CreateUserRequest owner
) {}
