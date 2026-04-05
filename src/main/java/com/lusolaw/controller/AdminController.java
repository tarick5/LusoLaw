package com.lusolaw.controller;

import com.lusolaw.dto.AdminDashboardResponse;
import com.lusolaw.dto.AdminIdentificationRecordResponse;
import com.lusolaw.dto.AdminLawyerReviewResponse;
import com.lusolaw.model.User;
import com.lusolaw.security.CurrentUserService;
import com.lusolaw.service.AdminAnalyticsService;
import com.lusolaw.service.AdminModerationService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminAnalyticsService adminAnalyticsService;
    private final CurrentUserService currentUserService;
    private final AdminModerationService adminModerationService;

    public AdminController(
            AdminAnalyticsService adminAnalyticsService,
            CurrentUserService currentUserService,
            AdminModerationService adminModerationService
    ) {
        this.adminAnalyticsService = adminAnalyticsService;
        this.currentUserService = currentUserService;
        this.adminModerationService = adminModerationService;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard(@RequestParam(name = "days", defaultValue = "30") int days) {
        requireAdmin();
        return adminAnalyticsService.buildDashboard(days);
    }

    @GetMapping("/lawyers/pending")
    public List<AdminLawyerReviewResponse> pendingLawyers() {
        requireAdmin();
        return adminModerationService.listPendingLawyers();
    }

    @PostMapping("/lawyers/{userId}/approve")
    public AdminLawyerReviewResponse approveLawyer(@PathVariable Long userId) {
        requireAdmin();
        return adminModerationService.approveLawyer(userId);
    }

    @PostMapping("/lawyers/{userId}/reject")
    public AdminLawyerReviewResponse rejectLawyer(@PathVariable Long userId) {
        requireAdmin();
        return adminModerationService.rejectLawyer(userId);
    }

    @GetMapping("/compliance/identifications")
    public List<AdminIdentificationRecordResponse> identificationRecords(
            @RequestParam(name = "limit", defaultValue = "30") int limit
    ) {
        requireAdmin();
        return adminModerationService.listIdentificationRecords(limit);
    }

    @GetMapping("/users/{userId}/documents/id")
    public ResponseEntity<ByteArrayResource> identificationDocument(@PathVariable Long userId) {
        requireAdmin();
        AdminModerationService.DocumentPayload payload = adminModerationService.readDocument(
                userId,
                AdminModerationService.DocumentKind.IDENTIFICATION
        );
        return documentResponse(payload);
    }

    @GetMapping("/users/{userId}/documents/lawyer-credential")
    public ResponseEntity<ByteArrayResource> lawyerCredentialDocument(@PathVariable Long userId) {
        requireAdmin();
        AdminModerationService.DocumentPayload payload = adminModerationService.readDocument(
                userId,
                AdminModerationService.DocumentKind.LAWYER_CREDENTIAL
        );
        return documentResponse(payload);
    }

    private void requireAdmin() {
        currentUserService.requireRole(User.Role.ADMIN);
    }

    private ResponseEntity<ByteArrayResource> documentResponse(AdminModerationService.DocumentPayload payload) {
        ByteArrayResource resource = new ByteArrayResource(payload.bytes());
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(payload.contentType());
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(payload.filename()).build().toString())
                .contentLength(payload.bytes().length)
                .body(resource);
    }
}
