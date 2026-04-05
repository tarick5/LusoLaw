package com.lusolaw.dto;

import com.lusolaw.model.User;

import java.time.LocalDateTime;

public record AdminIdentificationRecordResponse(
        Long userId,
        String name,
        String email,
        User.Role role,
        String identificationNumber,
        User.AccountStatus accountStatus,
        LocalDateTime createdAt,
        boolean hasIdentificationDocument,
        boolean hasLawyerCredentialDocument
) {
}
