package com.valiantech.core.iam.util;

import com.valiantech.core.iam.company.dto.CompanyResponse;
import com.valiantech.core.iam.company.model.CompanyStatus;
import com.valiantech.core.iam.exception.ConflictException;
import com.valiantech.core.iam.user.dto.UserResponse;
import com.valiantech.core.iam.usercompany.model.UserCompanyRole;
import com.valiantech.core.iam.user.model.UserStatus;
import com.valiantech.core.iam.usercompany.model.UserCompany;

import java.util.Arrays;

public class ValidationUtils {

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
