package com.lusolaw.dto;

import jakarta.validation.constraints.NotNull;

public record BookingDecisionRequest(
        @NotNull(message = "Decisao e obrigatoria")
        Decision decision
) {
    public enum Decision {
        ACCEPT,
        REJECT
    }
}
