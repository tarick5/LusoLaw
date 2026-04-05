package com.lusolaw.service;

import com.lusolaw.model.Booking;
import com.lusolaw.model.BookingStatus;
import com.lusolaw.repository.BookingRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    @Scheduled(fixedRate = 3_600_000)
    public void checkPendingBookings() {
        List<Booking> expired = bookingRepository.findExpiredPendingBookings();
        for (Booking booking : expired) {
            booking.setStatus(BookingStatus.REFUNDED);
            booking.setRespondedAt(LocalDateTime.now());
            bookingRepository.save(booking);
        }
    }
}
