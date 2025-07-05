package com.valiantech.core.iam.auth.service;

import com.valiantech.core.iam.auth.dto.LoginRequest;
import com.valiantech.core.iam.auth.dto.LoginResponse;
import com.valiantech.core.iam.exception.UnauthorizedException;
import com.valiantech.core.iam.user.dto.UserResponse;
import com.valiantech.core.iam.user.model.User;
import com.valiantech.core.iam.user.model.UserStatus;
import com.valiantech.core.iam.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService; // lo verás abajo

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        if (!user.getStatus().equals(UserStatus.ACTIVE)) {
            throw new UnauthorizedException("User is not active");
        }

        String token = jwtService.generateToken(user); // método explicado más abajo

        return new LoginResponse(token, null, UserResponse.from(user));
    }
}
