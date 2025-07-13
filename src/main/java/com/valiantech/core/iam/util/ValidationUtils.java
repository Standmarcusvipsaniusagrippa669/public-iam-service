package com.valiantech.core.iam.util;

import com.valiantech.core.iam.company.dto.CompanyResponse;
import com.valiantech.core.iam.company.model.CompanyStatus;
import com.valiantech.core.iam.exception.ConflictException;
import com.valiantech.core.iam.user.dto.UserResponse;
import com.valiantech.core.iam.usercompany.model.UserCompanyRole;
import com.valiantech.core.iam.user.model.UserStatus;
import com.valiantech.core.iam.usercompany.model.UserCompany;

import java.util.Arrays;

/**
 * Utilidades de validación para reglas de negocio en el sistema IAM.
 * <ul>
 *   <li>Permite validar estado activo de compañías y usuarios.</li>
 *   <li>Permite validar que un usuario-empresa tenga alguno de los roles permitidos.</li>
 *   <li>Lanza {@link ConflictException} en caso de incumplimiento de reglas.</li>
 * </ul>
 * <b>Notas:</b>
 * <ul>
 *   <li>Es estática, no requiere instanciarse.</li>
 *   <li>Ideal para validar pre-condiciones en servicios y controladores.</li>
 * </ul>
 * @author Ian Cardenas
 * @since 1.0
 */
public class ValidationUtils {
    ValidationUtils() {
        // empty constructor
    }

    public static void validateCompanyIsActive(CompanyResponse company) {
        if (company.status() != CompanyStatus.ACTIVE) {
            throw new ConflictException("Company is not active");
        }
    }

    public static void validateUserIsActive(UserResponse user) {
        if (user.status() != UserStatus.ACTIVE) {
            throw new ConflictException("User is not active");
        }
    }

    public static void validateUserHasRole(UserCompany userCompany, UserCompanyRole... allowedRoles) {
        if (!Arrays.asList(allowedRoles).contains(userCompany.getRole())) {
            throw new ConflictException("User does not have required permission");
        }
    }
}
