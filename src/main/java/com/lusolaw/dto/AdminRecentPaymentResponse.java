package com.lusolaw.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminRecentPaymentResponse(
        Long bookingId,
        String serviceName,
        String clientName,
        String lawyerName,
        BigDecimal amount,
        LocalDateTime paidAt
) {
}
