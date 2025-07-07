package com.valiantech.core.iam.auth.service;

import com.valiantech.core.iam.auth.dto.*;
import com.valiantech.core.iam.auth.model.LoginTicket;
import com.valiantech.core.iam.auth.repository.LoginTicketRepository;
import com.valiantech.core.iam.company.model.Company;
import com.valiantech.core.iam.company.repository.CompanyRepository;
import com.valiantech.core.iam.exception.UnauthorizedException;
import com.valiantech.core.iam.user.dto.UserResponse;
import com.valiantech.core.iam.user.model.User;
import com.valiantech.core.iam.user.model.UserStatus;
import com.valiantech.core.iam.user.repository.UserRepository;
import com.valiantech.core.iam.usercompany.model.UserCompany;
import com.valiantech.core.iam.usercompany.model.UserCompanyStatus;
import com.valiantech.core.iam.usercompany.repository.UserCompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid credentials";
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserCompanyRepository userCompanyRepository;
    private final CompanyRepository companyRepository;
    private final LoginTicketRepository loginTicketRepository;

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

    public LoginResponse loginWithCompany(TokenRequest request) {
        LoginTicket ticket = loginTicketRepository.findById(request.loginTicket())
                .orElseThrow(() -> new UnauthorizedException("Invalid login ticket"));

        if (ticket.isUsed() || ticket.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Ticket expired or already used");
        }
        if (!ticket.getEmail().equalsIgnoreCase(request.email())) {
            throw new UnauthorizedException("Ticket does not match user");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));

        UserCompany userCompany = userCompanyRepository.findByUserIdAndCompanyId(user.getId(), request.companyId())
                .orElseThrow(() -> new UnauthorizedException("Not affiliated to this company"));

        if (!userCompany.getStatus().equals(UserCompanyStatus.ACTIVE)) {
            throw new UnauthorizedException("Not active in this company");
        }

        String role = userCompany.getRole().name();
        String token = jwtService.generateToken(user, request.companyId(), role);

        // Invalida el ticket
        ticket.setUsed(true);
        loginTicketRepository.save(ticket);

        return new LoginResponse(token, UserResponse.from(user), request.companyId(), role);
    }
}
