package com.lusolaw.loader;

import com.lusolaw.model.User;
import com.lusolaw.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AdminBootstrapLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapLoader.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.bootstrap.enabled:true}")
    private boolean adminBootstrapEnabled;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.name:Platform Admin}")
    private String adminName;

    public AdminBootstrapLoader(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!adminBootstrapEnabled) {
            return;
        }

        String normalizedEmail = normalize(adminEmail);
        if (normalizedEmail == null || adminPassword == null || adminPassword.isBlank()) {
            log.info("Admin bootstrap ignorado: app.admin.email/app.admin.password nao configurados.");
            return;
        }

        if (adminPassword.length() < 12) {
            throw new IllegalStateException("app.admin.password deve ter no minimo 12 caracteres.");
        }

        userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresentOrElse(existing -> {
            if (existing.getRole() != User.Role.ADMIN) {
                log.warn("Email de bootstrap admin ja existe com outro perfil: {}", normalizedEmail);
            }
        }, () -> {
            User admin = new User();
            admin.setName(normalize(adminName) == null ? "Platform Admin" : adminName.trim());
            admin.setEmail(normalizedEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(User.Role.ADMIN);
            admin.setAccountStatus(User.AccountStatus.ACTIVE);
            userRepository.save(admin);
            log.info("Conta ADMIN criada para {}", normalizedEmail);
        });
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }
}
