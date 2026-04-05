package com.lusolaw.dto;

import com.lusolaw.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserSummaryResponse(
        Long id,
        String name,
        String email,
        User.Role role,
        String phone,
        String address,
        String specialization,
        String lawyerRegistrationNumber,
        User.AccountStatus accountStatus,
        BigDecimal pricePerHour,
        LocalDateTime createdAt
) {
}
