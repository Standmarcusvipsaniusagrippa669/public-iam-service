package com.valiantech.core.iam.company.repository;

import com.valiantech.core.iam.company.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    Optional<Company> findByRut(String rut);
}
