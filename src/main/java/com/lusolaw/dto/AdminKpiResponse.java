package com.lusolaw.dto;

import java.math.BigDecimal;

public record AdminKpiResponse(
        long totalUsers,
        long totalLawyers,
        long totalClients,
        long totalAdmins,
        long totalServices,
        long totalBookings,
        long totalPaidBookings,
        long pendingBookings,
        long acceptedBookings,
        long rejectedBookings,
        long refundedBookings,
        long paidBookingsInWindow,
        BigDecimal totalRevenue,
        BigDecimal revenueInWindow,
        BigDecimal averageTicket
) {
}
