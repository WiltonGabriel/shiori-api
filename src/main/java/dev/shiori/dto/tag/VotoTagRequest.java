package dev.shiori.dto.tag;

import dev.shiori.domain.enums.TipoVoto;
import jakarta.validation.constraints.NotNull;

public record VotoTagRequest(
        @NotNull(message = "O tipo de voto (UPVOTE ou DOWNVOTE) é obrigatório")
        TipoVoto tipoVoto
) {}
