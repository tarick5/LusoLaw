package com.lusolaw.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServiceResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        LawyerResponse lawyer,
        LocalDateTime createdAt
) {
}
