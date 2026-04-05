package com.lusolaw.dto;

import java.math.BigDecimal;

public record AdminRevenuePointResponse(
        String month,
        BigDecimal revenue,
        long paidBookings
) {
}
