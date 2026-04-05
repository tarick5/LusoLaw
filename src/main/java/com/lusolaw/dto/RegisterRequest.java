package com.lusolaw.dto;

import com.lusolaw.model.User;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RegisterRequest(
        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 100, message = "Nome demasiado longo")
        String name,

        @NotBlank(message = "Email e obrigatorio")
        @Email(message = "Email invalido")
        @Size(max = 180, message = "Email demasiado longo")
        String email,

        @NotBlank(message = "Senha e obrigatoria")
        @Size(min = 8, max = 72, message = "Senha deve ter entre 8 e 72 caracteres")
        String password,

        User.Role role,

        @Size(max = 30, message = "Telefone demasiado longo")
        String phone,

        @Size(max = 180, message = "Morada demasiado longa")
        String address,

        @Size(max = 120, message = "Especializacao demasiado longa")
        String specialization,

        @DecimalMin(value = "0.0", inclusive = false, message = "Preco/hora deve ser maior que zero")
        @Digits(integer = 8, fraction = 2, message = "Preco/hora invalido")
        BigDecimal pricePerHour
) {
}
