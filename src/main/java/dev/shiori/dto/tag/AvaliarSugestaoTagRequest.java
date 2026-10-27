package dev.shiori.dto.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AvaliarSugestaoTagRequest(
        @NotBlank(message = "O motivo da rejeição é obrigatório")
        @Size(max = 255, message = "O motivo da rejeição não pode exceder 255 caracteres")
        String motivo
) {}
