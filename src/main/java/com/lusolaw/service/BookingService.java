package com.lusolaw.service;

import com.lusolaw.model.Booking;
import com.lusolaw.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Scheduled(fixedRate = 3600000) // Every hour
    public void checkPendingBookings() {
        List<Booking> expired = bookingRepository.findExpiredPendingBookings();
        for (Booking booking : expired) {
            booking.setStatus("REFUNDED");
            bookingRepository.save(booking);
            // Implement refund logic with Stripe
        }
    }
}