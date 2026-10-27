package dev.shiori.dto.tag;

import dev.shiori.domain.enums.TipoVoto;

public record VotoTagResponse(
        Long tagId,
        Long obraId,
        int scoreAtual,
        TipoVoto meuVoto
) {}
