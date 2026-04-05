package com.lusolaw.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @NotBlank
    @JsonIgnore
    @Column(nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.CLIENT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(length = 30)
    private String phone;

    @Column(length = 180)
    private String address;

    @Column(length = 120)
    private String specialization;

    @Column(length = 80)
    private String lawyerRegistrationNumber;

    @Column(length = 80)
    private String identificationNumber;

    @Column(length = 180)
    private String identificationDocumentFilename;

    @Column(length = 120)
    private String identificationDocumentContentType;

    private Long identificationDocumentSize;

    @Column(columnDefinition = "BYTEA")
    private byte[] identificationDocumentData;

    @Column(length = 180)
    private String lawyerCredentialFilename;

    @Column(length = 120)
    private String lawyerCredentialContentType;

    private Long lawyerCredentialSize;

    @Column(columnDefinition = "BYTEA")
    private byte[] lawyerCredentialData;

    @Column(precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum Role {
        CLIENT,
        LAWYER,
        ADMIN
    }

    public enum AccountStatus {
        ACTIVE,
        PENDING_REVIEW,
        REJECTED
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getLawyerRegistrationNumber() {
        return lawyerRegistrationNumber;
    }

    public void setLawyerRegistrationNumber(String lawyerRegistrationNumber) {
        this.lawyerRegistrationNumber = lawyerRegistrationNumber;
    }

    public String getIdentificationNumber() {
        return identificationNumber;
    }

    public void setIdentificationNumber(String identificationNumber) {
        this.identificationNumber = identificationNumber;
    }

    public String getIdentificationDocumentFilename() {
        return identificationDocumentFilename;
    }

    public void setIdentificationDocumentFilename(String identificationDocumentFilename) {
        this.identificationDocumentFilename = identificationDocumentFilename;
    }

    public String getIdentificationDocumentContentType() {
        return identificationDocumentContentType;
    }

    public void setIdentificationDocumentContentType(String identificationDocumentContentType) {
        this.identificationDocumentContentType = identificationDocumentContentType;
    }

    public Long getIdentificationDocumentSize() {
        return identificationDocumentSize;
    }

    public void setIdentificationDocumentSize(Long identificationDocumentSize) {
        this.identificationDocumentSize = identificationDocumentSize;
    }

    public byte[] getIdentificationDocumentData() {
        return identificationDocumentData;
    }

    public void setIdentificationDocumentData(byte[] identificationDocumentData) {
        this.identificationDocumentData = identificationDocumentData;
    }

    public String getLawyerCredentialFilename() {
        return lawyerCredentialFilename;
    }

    public void setLawyerCredentialFilename(String lawyerCredentialFilename) {
        this.lawyerCredentialFilename = lawyerCredentialFilename;
    }

    public String getLawyerCredentialContentType() {
        return lawyerCredentialContentType;
    }

    public void setLawyerCredentialContentType(String lawyerCredentialContentType) {
        this.lawyerCredentialContentType = lawyerCredentialContentType;
    }

    public Long getLawyerCredentialSize() {
        return lawyerCredentialSize;
    }

    public void setLawyerCredentialSize(Long lawyerCredentialSize) {
        this.lawyerCredentialSize = lawyerCredentialSize;
    }

    public byte[] getLawyerCredentialData() {
        return lawyerCredentialData;
    }

    public void setLawyerCredentialData(byte[] lawyerCredentialData) {
        this.lawyerCredentialData = lawyerCredentialData;
    }

    public BigDecimal getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(BigDecimal pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
