package com.lusolaw.dto;

import java.math.BigDecimal;

public record LawyerResponse(
        Long id,
        String name,
        String specialization,
        BigDecimal pricePerHour
) {
}
