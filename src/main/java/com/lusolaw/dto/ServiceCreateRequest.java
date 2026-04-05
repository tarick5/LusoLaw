package com.lusolaw.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ServiceCreateRequest(
        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 120, message = "Nome demasiado longo")
        String name,

        @NotBlank(message = "Descricao e obrigatoria")
        @Size(max = 500, message = "Descricao demasiado longa")
        String description,

        @DecimalMin(value = "0.0", inclusive = false, message = "Preco deve ser maior que zero")
        @Digits(integer = 8, fraction = 2, message = "Preco invalido")
        BigDecimal price
) {
}
