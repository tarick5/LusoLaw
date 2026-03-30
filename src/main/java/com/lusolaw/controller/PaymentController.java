package com.lusolaw.controller;

import com.lusolaw.model.Booking;
import com.lusolaw.repository.BookingRepository;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Autowired
    private BookingRepository bookingRepository;

    @PostMapping("/create-intent/{bookingId}")
    public ResponseEntity<?> createPaymentIntent(@PathVariable Long bookingId) {
        Stripe.apiKey = stripeApiKey;

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null || !"ACCEPTED".equals(booking.getStatus())) {
            return ResponseEntity.badRequest().body("Invalid booking");
        }

        double platformFee = booking.getAmount() * 0.20;
        long amount = (long) ((booking.getAmount() + platformFee) * 100); // in cents

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amount)
                .setCurrency("eur")
                .build();

        try {
            PaymentIntent intent = PaymentIntent.create(params);
            return ResponseEntity.ok(intent.getClientSecret());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Payment creation failed");
        }
    }
}