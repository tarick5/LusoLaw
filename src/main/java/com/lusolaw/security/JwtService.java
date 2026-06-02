package com.lusolaw.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-minutes:120}")
    private long expirationMinutes;

    @Value("${app.jwt.issuer:lusolaw}")
    private String jwtIssuer;

    private SecretKey secretKey;
    private JwtParser jwtParser;

    @PostConstruct
    void init() {
        boolean ephemeralKey = false;
        byte[] keyBytes;
        if (jwtSecret == null || jwtSecret.isBlank()) {
            keyBytes = new byte[64];
            new SecureRandom().nextBytes(keyBytes);
            ephemeralKey = true;
        } else {
            try {
                keyBytes = Decoders.BASE64.decode(jwtSecret);
            } catch (Exception ignored) {
                keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
            }
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret deve ter pelo menos 32 bytes");
        }

        secretKey = Keys.hmacShaKeyFor(keyBytes);
        jwtParser = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(jwtIssuer)
                .build();

        if (ephemeralKey) {
            log.warn("APP_JWT_SECRET nao definido. A usar chave JWT efemera para desenvolvimento local; tokens expiram apos reinicio.");
        }
    }

    public String generateToken(UserDetails userDetails, Map<String, Object> extraClaims) {
        Date now = Date.from(Instant.now());
        Date expiration = Date.from(getExpirationInstant());

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuer(jwtIssuer)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public Instant getExpirationInstant() {
        return Instant.now().plusSeconds(expirationMinutes * 60);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equalsIgnoreCase(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration.before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        Claims claims = jwtParser.parseSignedClaims(token).getPayload();
        return claimResolver.apply(claims);
    }
}
