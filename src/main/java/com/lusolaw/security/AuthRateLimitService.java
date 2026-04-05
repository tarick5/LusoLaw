package com.lusolaw.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@Service
public class AuthRateLimitService {

    private static final class AttemptBucket {
        private long windowStartMs;
        private int attempts;

        private AttemptBucket(long windowStartMs, int attempts) {
            this.windowStartMs = windowStartMs;
            this.attempts = attempts;
        }
    }

    private final Map<String, AttemptBucket> buckets = new ConcurrentHashMap<>();
    private final int loginMaxAttempts;
    private final Duration loginWindow;
    private final int registerMaxAttempts;
    private final Duration registerWindow;

    public AuthRateLimitService(
            @Value("${app.security.rate-limit.login.max-attempts:10}") int loginMaxAttempts,
            @Value("${app.security.rate-limit.login.window-minutes:15}") long loginWindowMinutes,
            @Value("${app.security.rate-limit.register.max-attempts:12}") int registerMaxAttempts,
            @Value("${app.security.rate-limit.register.window-minutes:60}") long registerWindowMinutes
    ) {
        this.loginMaxAttempts = loginMaxAttempts;
        this.loginWindow = Duration.ofMinutes(Math.max(1, loginWindowMinutes));
        this.registerMaxAttempts = registerMaxAttempts;
        this.registerWindow = Duration.ofMinutes(Math.max(1, registerWindowMinutes));
    }

    public void assertLoginAllowed(String clientIp, String email) {
        assertAllowed(
                loginKey(clientIp, email),
                loginMaxAttempts,
                loginWindow,
                "Muitas tentativas de login. Tente novamente mais tarde."
        );
    }

    public void recordLoginFailure(String clientIp, String email) {
        increment(loginKey(clientIp, email), loginWindow);
    }

    public void resetLoginFailures(String clientIp, String email) {
        buckets.remove(loginKey(clientIp, email));
    }

    public void consumeRegisterAttempt(String clientIp) {
        int attempts = incrementAndGet(registerKey(clientIp), registerWindow);
        if (attempts > registerMaxAttempts) {
            throw new ResponseStatusException(
                    TOO_MANY_REQUESTS,
                    "Limite de criacao de contas atingido. Tente novamente mais tarde."
            );
        }
    }

    @Scheduled(fixedDelay = 600_000)
    void evictExpired() {
        long now = System.currentTimeMillis();
        long retentionMs = Math.max(loginWindow.toMillis(), registerWindow.toMillis()) * 2;
        buckets.entrySet().removeIf(entry -> now - entry.getValue().windowStartMs > retentionMs);
    }

    private void assertAllowed(String key, int maxAttempts, Duration window, String message) {
        AttemptBucket bucket = buckets.get(key);
        if (bucket == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long windowMs = window.toMillis();

        if (now - bucket.windowStartMs >= windowMs) {
            buckets.remove(key, bucket);
            return;
        }

        if (bucket.attempts >= maxAttempts) {
            throw new ResponseStatusException(TOO_MANY_REQUESTS, message);
        }
    }

    private void increment(String key, Duration window) {
        incrementAndGet(key, window);
    }

    private int incrementAndGet(String key, Duration window) {
        long now = System.currentTimeMillis();
        long windowMs = window.toMillis();

        AttemptBucket updated = buckets.compute(key, (unused, current) -> {
            if (current == null || now - current.windowStartMs >= windowMs) {
                return new AttemptBucket(now, 1);
            }

            current.attempts += 1;
            return current;
        });

        return updated == null ? 0 : updated.attempts;
    }

    private String loginKey(String clientIp, String email) {
        return "login:" + safe(clientIp) + ":" + safe(email);
    }

    private String registerKey(String clientIp) {
        return "register:" + safe(clientIp);
    }

    private String safe(String value) {
        return value == null ? "unknown" : value.trim().toLowerCase();
    }
}
