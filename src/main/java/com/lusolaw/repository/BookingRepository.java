package com.lusolaw.repository;

import com.lusolaw.model.Booking;
import com.lusolaw.model.BookingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    interface LawyerRevenueProjection {
        Long getLawyerId();
        String getLawyerName();
        BigDecimal getRevenue();
        Long getPaidBookings();
    }

    @EntityGraph(attributePaths = {"client", "lawyer", "service", "service.lawyer"})
    List<Booking> findByLawyerIdOrderByRequestedAtDesc(Long lawyerId);

    @EntityGraph(attributePaths = {"client", "lawyer", "service", "service.lawyer"})
    List<Booking> findByClientIdOrderByRequestedAtDesc(Long clientId);

    @Override
    @EntityGraph(attributePaths = {"client", "lawyer", "service", "service.lawyer"})
    Optional<Booking> findById(Long id);

    @EntityGraph(attributePaths = {"client", "lawyer", "service", "service.lawyer"})
    Optional<Booking> findByStripePaymentIntentId(String stripePaymentIntentId);

    @Query("SELECT b FROM Booking b WHERE b.status = com.lusolaw.model.BookingStatus.PENDING AND b.deadline < CURRENT_TIMESTAMP")
    List<Booking> findExpiredPendingBookings();

    long countByStatus(BookingStatus status);

    long countByStatusAndPaidAtGreaterThanEqual(BookingStatus status, LocalDateTime paidAt);

    @Query("SELECT COALESCE(SUM(b.amount), 0) FROM Booking b WHERE b.status = com.lusolaw.model.BookingStatus.PAID")
    BigDecimal sumPaidRevenueTotal();

    @Query("""
            SELECT COALESCE(SUM(b.amount), 0) FROM Booking b
            WHERE b.status = com.lusolaw.model.BookingStatus.PAID
            AND b.paidAt >= :since
            """)
    BigDecimal sumPaidRevenueSince(@Param("since") LocalDateTime since);

    @EntityGraph(attributePaths = {"client", "lawyer", "service", "service.lawyer"})
    List<Booking> findByStatusAndPaidAtGreaterThanEqualOrderByPaidAtDesc(BookingStatus status, LocalDateTime paidAt);

    @EntityGraph(attributePaths = {"client", "lawyer", "service", "service.lawyer"})
    List<Booking> findByStatusAndPaidAtIsNotNullOrderByPaidAtDesc(BookingStatus status, Pageable pageable);

    @Query("""
            SELECT b.lawyer.id AS lawyerId,
                   b.lawyer.name AS lawyerName,
                   COALESCE(SUM(b.amount), 0) AS revenue,
                   COUNT(b.id) AS paidBookings
            FROM Booking b
            WHERE b.status = com.lusolaw.model.BookingStatus.PAID
            GROUP BY b.lawyer.id, b.lawyer.name
            ORDER BY COALESCE(SUM(b.amount), 0) DESC
            """)
    List<LawyerRevenueProjection> findTopLawyersByRevenue(Pageable pageable);
}
