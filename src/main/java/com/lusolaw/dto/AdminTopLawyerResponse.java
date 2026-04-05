package com.lusolaw.dto;

import java.math.BigDecimal;

public record AdminTopLawyerResponse(
        Long lawyerId,
        String lawyerName,
        BigDecimal revenue,
        long paidBookings
) {
}
