package com.lusolaw.dto;

import com.lusolaw.model.User;

import java.time.LocalDateTime;

public record AdminLawyerReviewResponse(
        Long userId,
        String name,
        String email,
        String lawyerRegistrationNumber,
        String identificationNumber,
        User.AccountStatus accountStatus,
        LocalDateTime createdAt,
        boolean hasIdentificationDocument,
        boolean hasLawyerCredentialDocument
) {
}
