package com.bank.userservice.controller;

import com.bank.userservice.dto.request.CreateChangeRequest;
import com.bank.userservice.dto.request.ReviewChangeRequest;
import com.bank.userservice.dto.response.ChangeRequestResponse;
import com.bank.userservice.security.SecurityUtils;
import com.bank.userservice.service.ChangeRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/change-requests")
@RequiredArgsConstructor
public class ChangeRequestController {

    private final ChangeRequestService changeRequestService;

    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<ChangeRequestResponse> createRequest(
            @Valid @RequestBody CreateChangeRequest request,
            Authentication authentication) {

        Long userId =
                SecurityUtils.getCurrentUserId(authentication);

        ChangeRequestResponse response =
                changeRequestService.createRequest(
                        userId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/my")
    public ResponseEntity<List<ChangeRequestResponse>> getMyRequests(
            Authentication authentication) {

        Long userId =
                SecurityUtils.getCurrentUserId(authentication);

        return ResponseEntity.ok(
                changeRequestService.getMyRequests(userId)
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/pending")
    public ResponseEntity<List<ChangeRequestResponse>> getPendingRequests() {

        return ResponseEntity.ok(
                changeRequestService.getPendingRequests()
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{requestId}/review")
    public ResponseEntity<ChangeRequestResponse> reviewRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody ReviewChangeRequest request,
            Authentication authentication) {

        Long adminId =
                SecurityUtils.getCurrentUserId(authentication);

        ChangeRequestResponse response =
                changeRequestService.reviewRequest(
                        requestId,
                        adminId,
                        request
                );

        return ResponseEntity.ok(response);
    }
}