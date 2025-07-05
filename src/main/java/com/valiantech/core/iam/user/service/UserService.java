package com.valiantech.core.iam.user.service;

import com.valiantech.core.iam.exception.ConflictException;
import com.valiantech.core.iam.exception.NotFoundException;
import com.valiantech.core.iam.user.dto.*;
import com.valiantech.core.iam.user.model.User;
import com.valiantech.core.iam.user.model.UserStatus;
import com.valiantech.core.iam.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse registerInactiveUser(CreateUserRequest request) {
        checkEmailUnique(request.email());
        return map(userRepository.save(buildUserEntity(request, UserStatus.PENDING, false)));
    }

    public UserResponse registerValidatedUser(CreateUserRequest request) {
        checkEmailUnique(request.email());
        return map(userRepository.save(buildUserEntity(request, UserStatus.ACTIVE, true)));
    }

    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (request.fullName() != null && !request.fullName().equals(user.getFullName())) {
            user.setFullName(request.fullName());
        }

        if (request.email() != null && !request.email().equals(user.getEmail())) {
            // Verifica que no exista ese email en otro usuario
            userRepository.findByEmail(request.email())
                    .filter(u -> !u.getId().equals(id))
                    .ifPresent(u -> { throw new ConflictException("Email already registered"); });

            user.setEmail(request.email());
        }

        if (request.status() != null) {
            user.setStatus(request.status());
        }
        if (request.mustChangePassword() != null && !request.mustChangePassword().equals(user.getMustChangePassword())) {
            user.setMustChangePassword(request.mustChangePassword());
        }

        user.setUpdatedAt(Instant.now());
        return map(userRepository.save(user));
    }

    public UserResponse getUser(UUID id) {
        return userRepository.findById(id)
                .map(this::map)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public List<UserResponse> listAll() {
        return userRepository.findAll()
                .stream().map(this::map).toList();
    }

    private User buildUserEntity(CreateUserRequest request, UserStatus status, Boolean emailValidated) {
        return User.builder()
                .id(UUID.randomUUID())
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .emailValidated(emailValidated)
                .mustChangePassword(false)
                .status(status)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private void checkEmailUnique(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("Email already registered");
        }
    }

    private UserResponse map(User u) {
        return new UserResponse(
                u.getId(),
                u.getFullName(),
                u.getEmail(),
                u.getEmailValidated(),
                u.getStatus(),
                u.getLastLoginAt(),
                u.getCreatedAt(),
                u.getUpdatedAt()
        );
    }
}
