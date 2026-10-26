package dev.shiori.dto.tag;

import jakarta.validation.constraints.Size;

public record SugerirTagRequest(
        Long tagId,
        @Size(max = 60, message = "O nome da tag não pode exceder 60 caracteres")
        String nomeTag,
        @Size(max = 255, message = "A justificativa não pode exceder 255 caracteres")
        String justificativa
) {}
