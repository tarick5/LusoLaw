package com.lusolaw.dto;

import java.math.BigDecimal;

public record PaymentIntentResponse(
        String clientSecret,
        BigDecimal serviceAmount,
        BigDecimal lusoLawServiceFee,
        BigDecimal totalAmount
) {
}
