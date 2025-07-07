package com.valiantech.core.iam.user.controller;

import com.valiantech.core.iam.security.SecurityUtil;
import com.valiantech.core.iam.user.dto.*;
import com.valiantech.core.iam.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Operations related to system users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Update an existing user",
            description = "Updates the specified fields of an existing user.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User successfully updated"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<UserResponse> patch(
            @PathVariable UUID id,
            @RequestBody UpdateUserRequest request) {
        UUID companyId = SecurityUtil.getCompanyIdFromContext();
        return ResponseEntity.ok(userService.updateUser(id, companyId, request));
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Get user by ID",
            description = "Retrieves the user details for the given ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User found"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','VIEWER')")
    public ResponseEntity<UserResponse> get(
            @Parameter(description = "User unique identifier")
            @PathVariable UUID id) {
        UUID companyId = SecurityUtil.getCompanyIdFromContext();
        return ResponseEntity.ok(userService.getUserByCompanyId(id, companyId));
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "List all users",
            description = "Returns a list of all users in the system.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of users retrieved")
            }
    )
    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','VIEWER')")
    public ResponseEntity<List<UserResponse>> list() {
        UUID companyId = SecurityUtil.getCompanyIdFromContext();
        return ResponseEntity.ok(userService.listAll(companyId));
    }
}
