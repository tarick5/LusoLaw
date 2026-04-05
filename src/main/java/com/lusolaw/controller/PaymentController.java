package com.lusolaw.controller;

import com.lusolaw.dto.PaymentIntentResponse;
import com.lusolaw.model.Booking;
import com.lusolaw.model.BookingStatus;
import com.lusolaw.model.User;
import com.lusolaw.repository.BookingRepository;
import com.lusolaw.security.CurrentUserService;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.02");

    @Value("${stripe.api.key:}")
    private String stripeApiKey;

    @Value("${stripe.webhook.secret:}")
    private String stripeWebhookSecret;

    private final BookingRepository bookingRepository;
    private final CurrentUserService currentUserService;

    public PaymentController(BookingRepository bookingRepository, CurrentUserService currentUserService) {
        this.bookingRepository = bookingRepository;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/create-intent/{bookingId}")
    @Transactional
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(@PathVariable Long bookingId) {
        User client = currentUserService.requireRole(User.Role.CLIENT);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Pedido nao encontrado"));

        if (!booking.getClient().getId().equals(client.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "Nao pode pagar pedidos de outro cliente");
        }

        if (booking.getStatus() != BookingStatus.ACCEPTED) {
            throw new ResponseStatusException(BAD_REQUEST, "Apenas pedidos aceites podem ser pagos");
        }

        if (booking.getPaidAt() != null || booking.getStatus() == BookingStatus.PAID) {
            throw new ResponseStatusException(CONFLICT, "Este pedido ja foi pago");
        }

        if (stripeApiKey.isBlank() || stripeApiKey.contains("placeholder") || stripeApiKey.contains("your_stripe")) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Stripe nao configurado no servidor");
        }

        BigDecimal serviceAmount = booking.getAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal platformFee = serviceAmount.multiply(PLATFORM_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = serviceAmount.add(platformFee).setScale(2, RoundingMode.HALF_UP);

        Stripe.apiKey = stripeApiKey;

        if (booking.getStripePaymentIntentId() != null && !booking.getStripePaymentIntentId().isBlank()) {
            try {
                PaymentIntent existingIntent = PaymentIntent.retrieve(booking.getStripePaymentIntentId());
                return ResponseEntity.ok(new PaymentIntentResponse(
                        existingIntent.getClientSecret(),
                        serviceAmount,
                        platformFee,
                        totalAmount
                ));
            } catch (Exception ex) {
                throw new ResponseStatusException(BAD_GATEWAY, "Falha ao recuperar pagamento existente");
            }
        }

        long amountInCents;
        try {
            amountInCents = totalAmount.multiply(new BigDecimal("100")).longValueExact();
        } catch (ArithmeticException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Valor de pagamento invalido");
        }

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("eur")
                .putMetadata("bookingId", booking.getId().toString())
                .putMetadata("clientId", client.getId().toString())
                .putMetadata("lawyerId", booking.getLawyer().getId().toString())
                .build();

        try {
            RequestOptions requestOptions = RequestOptions.builder()
                    .setIdempotencyKey("booking-" + booking.getId())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params, requestOptions);
            booking.setStripePaymentIntentId(intent.getId());
            bookingRepository.save(booking);

            return ResponseEntity.ok(new PaymentIntentResponse(
                    intent.getClientSecret(),
                    serviceAmount,
                    platformFee,
                    totalAmount
            ));
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_GATEWAY, "Falha ao criar pagamento na Stripe");
        }
    }

    @PostMapping("/webhook")
    @Transactional
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String stripeSignature
    ) {
        if (stripeApiKey.isBlank() || stripeWebhookSecret.isBlank()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Webhook Stripe nao configurado no servidor");
        }

        if (stripeSignature == null || stripeSignature.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Assinatura Stripe ausente");
        }

        Stripe.apiKey = stripeApiKey;

        Event event;
        try {
            event = Webhook.constructEvent(payload, stripeSignature, stripeWebhookSecret);
        } catch (SignatureVerificationException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Assinatura Stripe invalida");
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Evento Stripe invalido");
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            Optional<com.stripe.model.StripeObject> object = event.getDataObjectDeserializer().getObject();
            if (object.isPresent() && object.get() instanceof PaymentIntent paymentIntent) {
                bookingRepository.findByStripePaymentIntentId(paymentIntent.getId()).ifPresent(booking -> {
                    if (booking.getStatus() == BookingStatus.ACCEPTED || booking.getStatus() == BookingStatus.PAID) {
                        booking.setStatus(BookingStatus.PAID);
                        if (booking.getPaidAt() == null) {
                            booking.setPaidAt(LocalDateTime.now());
                        }
                        bookingRepository.save(booking);
                    }
                });
            }
        }

        return ResponseEntity.ok().build();
    }
}
