package com.valiantech.core.iam.auth.controller;

import com.valiantech.core.iam.auth.dto.LoginRequest;
import com.valiantech.core.iam.auth.dto.AssociatedCompanies;
import com.valiantech.core.iam.auth.dto.LoginResponse;
import com.valiantech.core.iam.auth.dto.TokenRequest;
import com.valiantech.core.iam.auth.service.AuthService;
import com.valiantech.core.iam.ratelimit.RateLimit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @RateLimit(capacity = 5, refill = 10)
    public ResponseEntity<AssociatedCompanies> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.fetchCompanies(request));
    }

    @PostMapping("/login-with-company")
    @RateLimit(capacity = 5, refill = 10)
    public ResponseEntity<LoginResponse> loginWithCompany(@RequestBody TokenRequest request) {
        return ResponseEntity.ok(authService.loginWithCompany(request));
    }
}