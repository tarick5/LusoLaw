package com.lusolaw.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Email e obrigatorio")
        @Email(message = "Email invalido")
        @Size(max = 180, message = "Email demasiado longo")
        String email,

        @NotBlank(message = "Senha e obrigatoria")
        @Size(min = 8, max = 72, message = "Senha deve ter entre 8 e 72 caracteres")
        String password
) {
}
