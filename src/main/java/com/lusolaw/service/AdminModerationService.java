package com.lusolaw.service;

import com.lusolaw.dto.AdminIdentificationRecordResponse;
import com.lusolaw.dto.AdminLawyerReviewResponse;
import com.lusolaw.model.User;
import com.lusolaw.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AdminModerationService {

    public enum DocumentKind {
        IDENTIFICATION,
        LAWYER_CREDENTIAL
    }

    private final UserRepository userRepository;

    public AdminModerationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<AdminLawyerReviewResponse> listPendingLawyers() {
        return userRepository
                .findByRoleAndAccountStatusOrderByCreatedAtDesc(User.Role.LAWYER, User.AccountStatus.PENDING_REVIEW)
                .stream()
                .map(this::toLawyerReview)
                .toList();
    }

    public List<AdminIdentificationRecordResponse> listIdentificationRecords(int limit) {
        int safeLimit = Math.max(1, Math.min(200, limit));
        return userRepository
                .findByIdentificationDocumentDataIsNotNullOrderByCreatedAtDesc(PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toIdentificationRecord)
                .toList();
    }

    public AdminLawyerReviewResponse approveLawyer(Long userId) {
        User user = requireLawyer(userId);
        user.setAccountStatus(User.AccountStatus.ACTIVE);
        return toLawyerReview(userRepository.save(user));
    }

    public AdminLawyerReviewResponse rejectLawyer(Long userId) {
        User user = requireLawyer(userId);
        user.setAccountStatus(User.AccountStatus.REJECTED);
        return toLawyerReview(userRepository.save(user));
    }

    public DocumentPayload readDocument(Long userId, DocumentKind kind) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Utilizador nao encontrado"));

        return switch (kind) {
            case IDENTIFICATION -> buildPayload(
                    user.getIdentificationDocumentFilename(),
                    user.getIdentificationDocumentContentType(),
                    user.getIdentificationDocumentData()
            );
            case LAWYER_CREDENTIAL -> {
                if (user.getRole() != User.Role.LAWYER) {
                    throw new ResponseStatusException(BAD_REQUEST, "Documento de advogado so existe para role LAWYER");
                }
                yield buildPayload(
                        user.getLawyerCredentialFilename(),
                        user.getLawyerCredentialContentType(),
                        user.getLawyerCredentialData()
                );
            }
        };
    }

    private User requireLawyer(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Advogado nao encontrado"));
        if (user.getRole() != User.Role.LAWYER) {
            throw new ResponseStatusException(BAD_REQUEST, "Utilizador indicado nao e advogado");
        }
        return user;
    }

    private DocumentPayload buildPayload(String filename, String contentType, byte[] data) {
        if (data == null || data.length == 0) {
            throw new ResponseStatusException(NOT_FOUND, "Documento nao encontrado");
        }

        String safeFilename = (filename == null || filename.isBlank()) ? "document" : filename;
        String safeContentType = (contentType == null || contentType.isBlank()) ? "application/octet-stream" : contentType;
        return new DocumentPayload(safeFilename, safeContentType, data);
    }

    private AdminLawyerReviewResponse toLawyerReview(User user) {
        return new AdminLawyerReviewResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getLawyerRegistrationNumber(),
                user.getIdentificationNumber(),
                user.getAccountStatus(),
                user.getCreatedAt(),
                user.getIdentificationDocumentData() != null && user.getIdentificationDocumentData().length > 0,
                user.getLawyerCredentialData() != null && user.getLawyerCredentialData().length > 0
        );
    }

    private AdminIdentificationRecordResponse toIdentificationRecord(User user) {
        return new AdminIdentificationRecordResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getIdentificationNumber(),
                user.getAccountStatus(),
                user.getCreatedAt(),
                user.getIdentificationDocumentData() != null && user.getIdentificationDocumentData().length > 0,
                user.getLawyerCredentialData() != null && user.getLawyerCredentialData().length > 0
        );
    }

    public record DocumentPayload(
            String filename,
            String contentType,
            byte[] bytes
    ) {
    }
}
