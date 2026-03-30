package com.lusolaw.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private User client;

    @ManyToOne
    @JoinColumn(name = "lawyer_id")
    private User lawyer;

    @ManyToOne
    @JoinColumn(name = "service_id")
    private Service service;

    private String situation; // Immigration situation
    private String details;
    private Double amount; // Amount to pay
    private String status; // PENDING, ACCEPTED, REJECTED, REFUNDED

    private LocalDateTime requestedAt = LocalDateTime.now();
    private LocalDateTime respondedAt;
    private LocalDateTime deadline; // 48h from request

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getClient() { return client; }
    public void setClient(User client) { this.client = client; }
    public User getLawyer() { return lawyer; }
    public void setLawyer(User lawyer) { this.lawyer = lawyer; }
    public Service getService() { return service; }
    public void setService(Service service) { this.service = service; }
    public String getSituation() { return situation; }
    public void setSituation(String situation) { this.situation = situation; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
}