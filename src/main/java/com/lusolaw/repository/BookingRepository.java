package com.lusolaw.repository;

import com.lusolaw.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByLawyerIdAndStatus(Long lawyerId, String status);
    List<Booking> findByClientId(Long clientId);

    @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING' AND b.deadline < CURRENT_TIMESTAMP")
    List<Booking> findExpiredPendingBookings();
}