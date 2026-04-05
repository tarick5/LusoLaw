package com.lusolaw.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookingCreateRequest(
        @NotNull(message = "ServiceId e obrigatorio")
        Long serviceId,

        @NotBlank(message = "Situacao e obrigatoria")
        @Size(max = 180, message = "Situacao demasiado longa")
        String situation,

        @NotBlank(message = "Detalhes sao obrigatorios")
        @Size(max = 2000, message = "Detalhes demasiado longos")
        String details
) {
}
