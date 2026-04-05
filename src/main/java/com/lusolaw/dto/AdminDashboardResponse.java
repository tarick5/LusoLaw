package com.lusolaw.dto;

import java.time.Instant;
import java.util.List;

public record AdminDashboardResponse(
        Instant generatedAt,
        int windowDays,
        AdminKpiResponse kpis,
        List<AdminRevenuePointResponse> revenueByMonth,
        List<AdminTopLawyerResponse> topLawyersByRevenue,
        List<AdminRecentPaymentResponse> recentPayments
) {
}
