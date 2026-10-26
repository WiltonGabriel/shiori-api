package dev.shiori.dto.tag;

import dev.shiori.domain.enums.TipoVoto;

public record TagObraResponse(
        Long tagId,
        String nome,
        String slug,
        int score,
        TipoVoto meuVoto
) {}
