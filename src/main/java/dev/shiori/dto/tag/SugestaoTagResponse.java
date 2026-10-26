package dev.shiori.dto.tag;

import java.time.LocalDateTime;

import dev.shiori.domain.enums.StatusSugestaoTag;
import dev.shiori.domain.model.SugestaoTagObra;

public record SugestaoTagResponse(
        Long id,
        Long obraId,
        Long tagId,
        String nomeTag,
        String slugTag,
        StatusSugestaoTag status,
        String usuarioSugeridorApelido,
        String justificativa,
        LocalDateTime dataSugestao
) {
    public static SugestaoTagResponse fromEntity(SugestaoTagObra s) {
        String slug = s.getTag() != null ? s.getTag().getSlug() : s.gerarSlug();
        Long tagId = s.getTag() != null ? s.getTag().getId() : null;
        String sugeridorApelido = s.getUsuarioSugeridor() != null ? s.getUsuarioSugeridor().getApelidoOuEmail() : null;
        return new SugestaoTagResponse(
                s.getId(),
                s.getObra().getId(),
                tagId,
                s.getNomeTag(),
                slug,
                s.getStatus(),
                sugeridorApelido,
                s.getJustificativa(),
                s.getDataSugestao()
        );
    }
}
