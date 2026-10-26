package dev.shiori.dto.tag;

import java.time.LocalDateTime;

import dev.shiori.domain.enums.StatusSugestaoTag;
import dev.shiori.domain.model.SugestaoTagObra;

public record SugestaoTagAdminResponse(
        Long id,
        Long obraId,
        String obraTitulo,
        Long tagId,
        String nomeTag,
        String slugTag,
        StatusSugestaoTag status,
        String usuarioSugeridorApelido,
        String justificativa,
        LocalDateTime dataSugestao,
        String adminAvaliadorApelido,
        LocalDateTime dataAvaliacao,
        String motivoRejeicao
) {
    public static SugestaoTagAdminResponse fromEntity(SugestaoTagObra s) {
        String slug = s.getTag() != null ? s.getTag().getSlug() : s.gerarSlug();
        Long tagId = s.getTag() != null ? s.getTag().getId() : null;
        String sugeridor = s.getUsuarioSugeridor() != null ? s.getUsuarioSugeridor().getApelidoOuEmail() : "Anônimo";
        String avaliador = s.getAdminAvaliador() != null ? s.getAdminAvaliador().getApelidoOuEmail() : null;

        return new SugestaoTagAdminResponse(
                s.getId(),
                s.getObra().getId(),
                s.getObra().getTituloPrincipal(),
                tagId,
                s.getNomeTag(),
                slug,
                s.getStatus(),
                sugeridor,
                s.getJustificativa(),
                s.getDataSugestao(),
                avaliador,
                s.getDataAvaliacao(),
                s.getMotivoRejeicao()
        );
    }
}
