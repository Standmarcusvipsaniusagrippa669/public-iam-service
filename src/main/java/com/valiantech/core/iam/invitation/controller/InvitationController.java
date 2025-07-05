package com.valiantech.core.iam.invitation.controller;

import com.valiantech.core.iam.invitation.dto.*;
import com.valiantech.core.iam.invitation.service.InvitationService;
import com.valiantech.core.iam.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
@Tag(name = "Invitation Management", description = "Operations for user invitations")
public class InvitationController {

    private final InvitationService invitationService;

    @Operation(
            summary = "Create a new invitation",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Invitation created successfully")
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<InvitationResponse> create(@Valid @RequestBody CreateInvitationRequest request) {
        return ResponseEntity.ok(invitationService.create(request));
    }

    @Operation(
            summary = "Accept an invitation",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Invitation accepted successfully"),
                    @ApiResponse(responseCode = "409", description = "Invitation already accepted or expired"),
                    @ApiResponse(responseCode = "404", description = "Invitation not found")
            }
    )
    @PostMapping("/accept-and-register")
    public ResponseEntity<UserResponse> accept(@Valid @RequestBody AcceptAndRegisterRequest request) {
        return ResponseEntity.ok(invitationService.acceptAndRegister(request));
    }

    @GetMapping("/{token}")
    @Operation(
            summary = "Get invitation details by token",
            description = "Returns the invitation details for a given invitation token.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Invitation found"),
                    @ApiResponse(responseCode = "404", description = "Invitation not found")
            }
    )
    public ResponseEntity<InvitationResponse> getByToken(@PathVariable String token) {
        return ResponseEntity.ok(invitationService.getByToken(token));
    }

    @Operation(
            summary = "List all invitations",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List retrieved")
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<List<InvitationResponse>> list() {
        return ResponseEntity.ok(invitationService.listAll());
    }
}
