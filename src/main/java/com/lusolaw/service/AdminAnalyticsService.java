package com.lusolaw.service;

import com.lusolaw.dto.AdminDashboardResponse;
import com.lusolaw.dto.AdminKpiResponse;
import com.lusolaw.dto.AdminRecentPaymentResponse;
import com.lusolaw.dto.AdminRevenuePointResponse;
import com.lusolaw.dto.AdminTopLawyerResponse;
import com.lusolaw.model.Booking;
import com.lusolaw.model.BookingStatus;
import com.lusolaw.model.User;
import com.lusolaw.repository.BookingRepository;
import com.lusolaw.repository.ServiceRepository;
import com.lusolaw.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AdminAnalyticsService {

    private static final int MIN_WINDOW_DAYS = 1;
    private static final int MAX_WINDOW_DAYS = 365;
    private static final int REVENUE_MONTHS = 12;
    private static final int TOP_LAWYERS_LIMIT = 6;
    private static final int RECENT_PAYMENTS_LIMIT = 12;
    private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final BookingRepository bookingRepository;

    public AdminAnalyticsService(
            UserRepository userRepository,
            ServiceRepository serviceRepository,
            BookingRepository bookingRepository
    ) {
        this.userRepository = userRepository;
        this.serviceRepository = serviceRepository;
        this.bookingRepository = bookingRepository;
    }

    public AdminDashboardResponse buildDashboard(int requestedWindowDays) {
        int windowDays = Math.min(MAX_WINDOW_DAYS, Math.max(MIN_WINDOW_DAYS, requestedWindowDays));
        LocalDateTime windowStart = LocalDateTime.now().minusDays(windowDays);

        long totalUsers = userRepository.count();
        long totalLawyers = userRepository.countByRole(User.Role.LAWYER);
        long totalClients = userRepository.countByRole(User.Role.CLIENT);
        long totalAdmins = userRepository.countByRole(User.Role.ADMIN);
        long totalServices = serviceRepository.count();
        long totalBookings = bookingRepository.count();
        long totalPaidBookings = bookingRepository.countByStatus(BookingStatus.PAID);
        long pendingBookings = bookingRepository.countByStatus(BookingStatus.PENDING);
        long acceptedBookings = bookingRepository.countByStatus(BookingStatus.ACCEPTED);
        long rejectedBookings = bookingRepository.countByStatus(BookingStatus.REJECTED);
        long refundedBookings = bookingRepository.countByStatus(BookingStatus.REFUNDED);
        long paidBookingsInWindow = bookingRepository.countByStatusAndPaidAtGreaterThanEqual(BookingStatus.PAID, windowStart);

        BigDecimal totalRevenue = safeMoney(bookingRepository.sumPaidRevenueTotal());
        BigDecimal revenueInWindow = safeMoney(bookingRepository.sumPaidRevenueSince(windowStart));
        BigDecimal averageTicket = totalPaidBookings == 0
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(totalPaidBookings), 2, RoundingMode.HALF_UP);

        AdminKpiResponse kpis = new AdminKpiResponse(
                totalUsers,
                totalLawyers,
                totalClients,
                totalAdmins,
                totalServices,
                totalBookings,
                totalPaidBookings,
                pendingBookings,
                acceptedBookings,
                rejectedBookings,
                refundedBookings,
                paidBookingsInWindow,
                totalRevenue,
                revenueInWindow,
                averageTicket
        );

        return new AdminDashboardResponse(
                Instant.now(),
                windowDays,
                kpis,
                buildRevenueSeries(),
                buildTopLawyersByRevenue(),
                buildRecentPayments()
        );
    }

    private List<AdminRevenuePointResponse> buildRevenueSeries() {
        YearMonth firstMonth = YearMonth.now().minusMonths(REVENUE_MONTHS - 1L);
        LocalDateTime firstMonthDate = firstMonth.atDay(1).atStartOfDay();
        List<Booking> paidBookings = bookingRepository
                .findByStatusAndPaidAtGreaterThanEqualOrderByPaidAtDesc(BookingStatus.PAID, firstMonthDate);

        Map<YearMonth, MutableRevenuePoint> timeline = new LinkedHashMap<>();
        for (int i = 0; i < REVENUE_MONTHS; i++) {
            YearMonth month = firstMonth.plusMonths(i);
            timeline.put(month, new MutableRevenuePoint());
        }

        for (Booking booking : paidBookings) {
            if (booking.getPaidAt() == null) {
                continue;
            }
            YearMonth month = YearMonth.from(booking.getPaidAt());
            MutableRevenuePoint point = timeline.get(month);
            if (point == null) {
                continue;
            }
            point.revenue = point.revenue.add(safeMoney(booking.getAmount()));
            point.paidBookings++;
        }

        List<AdminRevenuePointResponse> output = new ArrayList<>();
        timeline.forEach((month, point) -> output.add(new AdminRevenuePointResponse(
                month.format(MONTH_LABEL_FORMATTER),
                point.revenue,
                point.paidBookings
        )));

        return output;
    }

    private List<AdminTopLawyerResponse> buildTopLawyersByRevenue() {
        return bookingRepository.findTopLawyersByRevenue(PageRequest.of(0, TOP_LAWYERS_LIMIT))
                .stream()
                .map(row -> new AdminTopLawyerResponse(
                        row.getLawyerId(),
                        row.getLawyerName(),
                        safeMoney(row.getRevenue()),
                        row.getPaidBookings() == null ? 0 : row.getPaidBookings()
                ))
                .toList();
    }

    private List<AdminRecentPaymentResponse> buildRecentPayments() {
        return bookingRepository
                .findByStatusAndPaidAtIsNotNullOrderByPaidAtDesc(BookingStatus.PAID, PageRequest.of(0, RECENT_PAYMENTS_LIMIT))
                .stream()
                .sorted(Comparator.comparing(Booking::getPaidAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(booking -> new AdminRecentPaymentResponse(
                        booking.getId(),
                        booking.getService() == null ? "-" : booking.getService().getName(),
                        booking.getClient() == null ? "-" : booking.getClient().getName(),
                        booking.getLawyer() == null ? "-" : booking.getLawyer().getName(),
                        safeMoney(booking.getAmount()),
                        booking.getPaidAt()
                ))
                .toList();
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private static final class MutableRevenuePoint {
        private BigDecimal revenue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private long paidBookings = 0;
    }
}
