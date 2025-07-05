package com.valiantech.core.iam.user.controller;

import com.valiantech.core.iam.user.dto.*;
import com.valiantech.core.iam.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Operations related to system users")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Create a new user",
            description = "Creates a new user with full name, email, and password.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User successfully created"),
                    @ApiResponse(responseCode = "400", description = "Invalid input data")
            }
    )
    @PostMapping
    public ResponseEntity<UserResponse> create(
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(userService.registerInactiveUser(request));
    }

    @Operation(
            summary = "Update an existing user",
            description = "Updates the specified fields of an existing user.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User successfully updated"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> patch(
            @PathVariable UUID id,
            @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @Operation(
            summary = "Get user by ID",
            description = "Retrieves the user details for the given ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User found"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> get(
            @Parameter(description = "User unique identifier")
            @PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @Operation(
            summary = "List all users",
            description = "Returns a list of all users in the system.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of users retrieved")
            }
    )
    @GetMapping
    public ResponseEntity<List<UserResponse>> list() {
        return ResponseEntity.ok(userService.listAll());
    }
}
