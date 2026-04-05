package com.lusolaw.controller;

import com.lusolaw.dto.ApiMessageResponse;
import com.lusolaw.dto.AuthResponse;
import com.lusolaw.dto.LoginRequest;
import com.lusolaw.mapper.ApiMapper;
import com.lusolaw.model.User;
import com.lusolaw.repository.UserRepository;
import com.lusolaw.security.AuthRateLimitService;
import com.lusolaw.security.CurrentUserService;
import com.lusolaw.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final long MAX_DOCUMENT_BYTES = 8L * 1024L * 1024L;
    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;
    private final AuthRateLimitService authRateLimitService;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService,
            JwtService jwtService,
            CurrentUserService currentUserService,
            AuthRateLimitService authRateLimitService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.currentUserService = currentUserService;
        this.authRateLimitService = authRateLimitService;
    }

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> register(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) User.Role role,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) BigDecimal pricePerHour,
            @RequestParam(required = false) String lawyerRegistrationNumber,
            @RequestParam(required = false) String identificationNumber,
            @RequestPart("identificationDocument") MultipartFile identificationDocument,
            @RequestPart(value = "lawyerCredentialDocument", required = false) MultipartFile lawyerCredentialDocument,
            HttpServletRequest httpRequest
    ) {
        String clientIp = getClientIp(httpRequest);
        authRateLimitService.consumeRegisterAttempt(clientIp);

        String normalizedEmail = normalizeEmail(email);
        validateBasicFields(name, password, normalizedEmail);

        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "Email ja registado");
        }

        User.Role requestedRole = role == null ? User.Role.CLIENT : role;
        if (requestedRole == User.Role.ADMIN) {
            throw new ResponseStatusException(BAD_REQUEST, "Perfil ADMIN nao pode ser criado por registo publico");
        }

        validateIdentification(identificationNumber, identificationDocument);

        if (requestedRole == User.Role.LAWYER) {
            validateLawyerFields(specialization, pricePerHour, lawyerRegistrationNumber, lawyerCredentialDocument);
        }

        User user = new User();
        user.setName(name.trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(requestedRole);
        user.setPhone(trimToNull(phone));
        user.setAddress(trimToNull(address));
        user.setSpecialization(trimToNull(specialization));
        user.setPricePerHour(pricePerHour);
        user.setLawyerRegistrationNumber(trimToNull(lawyerRegistrationNumber));
        user.setIdentificationNumber(trimToNull(identificationNumber));
        user.setAccountStatus(requestedRole == User.Role.LAWYER
                ? User.AccountStatus.PENDING_REVIEW
                : User.AccountStatus.ACTIVE);

        storeDocument(
                identificationDocument,
                user::setIdentificationDocumentFilename,
                user::setIdentificationDocumentContentType,
                user::setIdentificationDocumentSize,
                user::setIdentificationDocumentData
        );

        if (requestedRole == User.Role.LAWYER && lawyerCredentialDocument != null && !lawyerCredentialDocument.isEmpty()) {
            storeDocument(
                    lawyerCredentialDocument,
                    user::setLawyerCredentialFilename,
                    user::setLawyerCredentialContentType,
                    user::setLawyerCredentialSize,
                    user::setLawyerCredentialData
            );
        }

        User saved = userRepository.save(user);

        if (saved.getRole() == User.Role.LAWYER) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ApiMessageResponse(
                    "Conta de advogado criada. Aguardando aprovacao do administrador."
            ));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(buildAuthResponse(saved));
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiMessageResponse> rejectLegacyJsonRegister() {
        throw new ResponseStatusException(
                BAD_REQUEST,
                "Registo requer documentos. Envie multipart/form-data com identificacao."
        );
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = getClientIp(httpRequest);
        String email = normalizeEmail(request.email());
        authRateLimitService.assertLoginAllowed(clientIp, email);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );
            authRateLimitService.resetLoginFailures(clientIp, email);
        } catch (AuthenticationException ex) {
            authRateLimitService.recordLoginFailure(clientIp, email);
            throw ex;
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas"));

        enforceAccountIsActiveForLogin(user);

        return ResponseEntity.ok(buildAuthResponse(user));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        User user = currentUserService.requireAuthenticatedUser();
        return ResponseEntity.ok(ApiMapper.toUserSummary(user));
    }

    private AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails, Map.of(
                "role", user.getRole().name(),
                "uid", user.getId()
        ));

        return new AuthResponse(token, "Bearer", jwtService.getExpirationInstant(), ApiMapper.toUserSummary(user));
    }

    private void enforceAccountIsActiveForLogin(User user) {
        if (user.getRole() != User.Role.LAWYER) {
            return;
        }

        if (user.getAccountStatus() == User.AccountStatus.PENDING_REVIEW) {
            throw new ResponseStatusException(FORBIDDEN, "Conta de advogado pendente de aprovacao");
        }
        if (user.getAccountStatus() == User.AccountStatus.REJECTED) {
            throw new ResponseStatusException(FORBIDDEN, "Conta de advogado rejeitada. Contacte o suporte.");
        }
    }

    private void validateBasicFields(String name, String password, String normalizedEmail) {
        if (!StringUtils.hasText(name) || name.trim().length() > 100) {
            throw new ResponseStatusException(BAD_REQUEST, "Nome invalido");
        }

        if (!StringUtils.hasText(password) || password.length() < 8 || password.length() > 72) {
            throw new ResponseStatusException(BAD_REQUEST, "Senha deve ter entre 8 e 72 caracteres");
        }

        if (!StringUtils.hasText(normalizedEmail) || normalizedEmail.length() > 180 || !normalizedEmail.contains("@")) {
            throw new ResponseStatusException(BAD_REQUEST, "Email invalido");
        }
    }

    private void validateIdentification(String identificationNumber, MultipartFile identificationDocument) {
        if (!StringUtils.hasText(identificationNumber) || identificationNumber.trim().length() > 80) {
            throw new ResponseStatusException(BAD_REQUEST, "Numero de documento de identificacao e obrigatorio");
        }

        validateDocumentFile(identificationDocument, "Documento de identificacao invalido");
    }

    private void validateLawyerFields(
            String specialization,
            BigDecimal pricePerHour,
            String lawyerRegistrationNumber,
            MultipartFile lawyerCredentialDocument
    ) {
        if (!StringUtils.hasText(specialization) || specialization.trim().length() > 120) {
            throw new ResponseStatusException(BAD_REQUEST, "Especializacao e obrigatoria para advogados");
        }

        if (pricePerHour == null || pricePerHour.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Preco/hora deve ser maior que zero");
        }

        if (!StringUtils.hasText(lawyerRegistrationNumber) || lawyerRegistrationNumber.trim().length() > 80) {
            throw new ResponseStatusException(BAD_REQUEST, "Numero de advogado e obrigatorio");
        }

        validateDocumentFile(lawyerCredentialDocument, "Comprovativo da ordem de advogados invalido");
    }

    private void validateDocumentFile(MultipartFile file, String invalidMessage) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, invalidMessage);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_DOCUMENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(BAD_REQUEST, "Formato de ficheiro nao suportado. Use PDF/JPG/PNG/WEBP.");
        }

        if (file.getSize() <= 0 || file.getSize() > MAX_DOCUMENT_BYTES) {
            throw new ResponseStatusException(BAD_REQUEST, "Ficheiro excede tamanho maximo de 8MB");
        }
    }

    private void storeDocument(
            MultipartFile file,
            java.util.function.Consumer<String> setFilename,
            java.util.function.Consumer<String> setContentType,
            java.util.function.Consumer<Long> setSize,
            java.util.function.Consumer<byte[]> setBytes
    ) {
        try {
            setFilename.accept(sanitizeFileName(file.getOriginalFilename()));
            setContentType.accept(file.getContentType());
            setSize.accept(file.getSize());
            setBytes.accept(file.getBytes());
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Falha ao processar upload de ficheiro");
        }
    }

    private String sanitizeFileName(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "document";
        }
        String cleaned = originalFilename.replaceAll("[\\r\\n\\\\/]", "_");
        return cleaned.length() > 180 ? cleaned.substring(0, 180) : cleaned;
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            return parts[0].trim();
        }

        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null || remoteAddr.isBlank() ? "unknown" : remoteAddr;
    }
}
