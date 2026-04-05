package com.lusolaw.dto;

import com.lusolaw.model.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        ServiceResponse service,
        UserSummaryResponse client,
        LawyerResponse lawyer,
        String situation,
        String details,
        BigDecimal amount,
        BookingStatus status,
        LocalDateTime requestedAt,
        LocalDateTime respondedAt,
        LocalDateTime deadline,
        LocalDateTime paidAt
) {
}
