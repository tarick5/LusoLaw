package com.lusolaw.controller;

import com.lusolaw.model.Booking;
import com.lusolaw.model.Service;
import com.lusolaw.model.User;
import com.lusolaw.repository.BookingRepository;
import com.lusolaw.repository.ServiceRepository;
import com.lusolaw.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @PostMapping("/request")
    public ResponseEntity<?> requestBooking(@RequestBody BookingRequest request) {
        User client = userRepository.findById(request.getClientId()).orElse(null);
        User lawyer = userRepository.findById(request.getLawyerId()).orElse(null);
        Service service = serviceRepository.findById(request.getServiceId()).orElse(null);

        if (client == null || lawyer == null || service == null) {
            return ResponseEntity.badRequest().body("Invalid IDs");
        }

        Booking booking = new Booking();
        booking.setClient(client);
        booking.setLawyer(lawyer);
        booking.setService(service);
        booking.setSituation(request.getSituation());
        booking.setDetails(request.getDetails());
        booking.setAmount(service.getPrice());
        booking.setStatus("PENDING");
        booking.setDeadline(LocalDateTime.now().plusHours(48));

        bookingRepository.save(booking);
        return ResponseEntity.ok("Booking requested");
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<?> respondToBooking(@PathVariable Long id, @RequestParam String response) {
        Booking booking = bookingRepository.findById(id).orElse(null);
        if (booking == null) return ResponseEntity.notFound().build();

        booking.setStatus(response.equals("accept") ? "ACCEPTED" : "REJECTED");
        booking.setRespondedAt(LocalDateTime.now());
        bookingRepository.save(booking);
        return ResponseEntity.ok("Response recorded");
    }

    @GetMapping("/lawyer/{lawyerId}")
    public List<Booking> getBookingsForLawyer(@PathVariable Long lawyerId) {
        return bookingRepository.findByLawyerIdAndStatus(lawyerId, "PENDING");
    }

    public static class BookingRequest {
        private Long clientId;
        private Long lawyerId;
        private Long serviceId;
        private String situation;
        private String details;
        // getters/setters
        public Long getClientId() { return clientId; }
        public void setClientId(Long clientId) { this.clientId = clientId; }
        public Long getLawyerId() { return lawyerId; }
        public void setLawyerId(Long lawyerId) { this.lawyerId = lawyerId; }
        public Long getServiceId() { return serviceId; }
        public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
        public String getSituation() { return situation; }
        public void setSituation(String situation) { this.situation = situation; }
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
    }
}