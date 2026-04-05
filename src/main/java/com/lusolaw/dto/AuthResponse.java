package com.lusolaw.dto;

import java.time.Instant;

public record AuthResponse(
        String token,
        String tokenType,
        Instant expiresAt,
        UserSummaryResponse user
) {
}
