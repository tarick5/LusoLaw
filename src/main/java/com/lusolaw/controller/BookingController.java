package com.lusolaw.controller;

import com.lusolaw.dto.BookingCreateRequest;
import com.lusolaw.dto.BookingDecisionRequest;
import com.lusolaw.dto.BookingResponse;
import com.lusolaw.mapper.ApiMapper;
import com.lusolaw.model.Booking;
import com.lusolaw.model.BookingStatus;
import com.lusolaw.model.Service;
import com.lusolaw.model.User;
import com.lusolaw.repository.BookingRepository;
import com.lusolaw.repository.ServiceRepository;
import com.lusolaw.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;
    private final CurrentUserService currentUserService;

    public BookingController(
            BookingRepository bookingRepository,
            ServiceRepository serviceRepository,
            CurrentUserService currentUserService
    ) {
        this.bookingRepository = bookingRepository;
        this.serviceRepository = serviceRepository;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> requestBooking(@Valid @RequestBody BookingCreateRequest request) {
        User client = currentUserService.requireRole(User.Role.CLIENT);

        Service service = serviceRepository.findById(request.serviceId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Servico nao encontrado"));

        Booking booking = new Booking();
        booking.setClient(client);
        booking.setLawyer(service.getLawyer());
        booking.setService(service);
        booking.setSituation(request.situation().trim());
        booking.setDetails(request.details().trim());
        booking.setAmount(service.getPrice());
        booking.setStatus(BookingStatus.PENDING);
        booking.setDeadline(LocalDateTime.now().plusHours(48));

        Booking saved = bookingRepository.save(booking);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiMapper.toBooking(saved));
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<BookingResponse> respondToBooking(
            @PathVariable Long id,
            @Valid @RequestBody BookingDecisionRequest request
    ) {
        User lawyer = currentUserService.requireRole(User.Role.LAWYER);

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Pedido nao encontrado"));

        if (!booking.getLawyer().getId().equals(lawyer.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "Nao pode responder pedidos de outro advogado");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(CONFLICT, "Este pedido ja foi processado");
        }

        if (booking.getDeadline().isBefore(LocalDateTime.now())) {
            booking.setStatus(BookingStatus.REFUNDED);
            booking.setRespondedAt(LocalDateTime.now());
            bookingRepository.save(booking);
            throw new ResponseStatusException(CONFLICT, "Prazo expirado. Pedido marcado como reembolsado");
        }

        booking.setStatus(request.decision() == BookingDecisionRequest.Decision.ACCEPT
                ? BookingStatus.ACCEPTED
                : BookingStatus.REJECTED);
        booking.setRespondedAt(LocalDateTime.now());

        Booking saved = bookingRepository.save(booking);
        return ResponseEntity.ok(ApiMapper.toBooking(saved));
    }

    @GetMapping("/me")
    public List<BookingResponse> myBookings() {
        User user = currentUserService.requireAuthenticatedUser();

        if (user.getRole() == User.Role.LAWYER) {
            return bookingRepository.findByLawyerIdOrderByRequestedAtDesc(user.getId()).stream()
                    .map(ApiMapper::toBooking)
                    .toList();
        }

        return bookingRepository.findByClientIdOrderByRequestedAtDesc(user.getId()).stream()
                .map(ApiMapper::toBooking)
                .toList();
    }

    @GetMapping("/lawyer/{lawyerId}")
    public List<BookingResponse> bookingsForLawyer(@PathVariable Long lawyerId) {
        User lawyer = currentUserService.requireRole(User.Role.LAWYER);

        if (!lawyer.getId().equals(lawyerId)) {
            throw new ResponseStatusException(FORBIDDEN, "Nao pode aceder a pedidos de outro advogado");
        }

        return bookingRepository.findByLawyerIdOrderByRequestedAtDesc(lawyerId).stream()
                .map(ApiMapper::toBooking)
                .toList();
    }
}
