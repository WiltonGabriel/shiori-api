package dev.shiori.dto.admin;

import java.time.LocalDateTime;

import dev.shiori.domain.enums.StatusSugestao;
import dev.shiori.domain.enums.TipoObra;
import dev.shiori.domain.model.SugestaoEdicaoObra;

public record SugestaoEdicaoResponse(
        Long id,
        Long obraId,
        String obraTitulo,
        String obraTituloOriginalAtual,
        String obraCapaUrlAtual,
        String obraSinopseAtual,
        Integer obraTotalCapitulosAtual,
        TipoObra obraTipoObraAtual,
        Long usuarioSugeridorId,
        String usuarioSugeridorApelido,
        String tituloPrincipalSugerido,
        String tituloOriginalSugerido,
        String capaUrlSugerida,
        String sinopseSugerida,
        Integer totalCapitulosSugerido,
        TipoObra tipoObraSugerido,
        String justificativa,
        StatusSugestao status,
        LocalDateTime dataCriacao,
        LocalDateTime dataAvaliacao,
        String parecerAdmin
) {
    public static SugestaoEdicaoResponse deEntidade(SugestaoEdicaoObra s) {
        return new SugestaoEdicaoResponse(
                s.getId(),
                s.getObra() != null ? s.getObra().getId() : null,
                s.getObra() != null ? s.getObra().getTituloPrincipal() : null,
                s.getObra() != null ? s.getObra().getTituloOriginal() : null,
                s.getObra() != null ? s.getObra().getCapaUrl() : null,
                s.getObra() != null ? s.getObra().getSinopse() : null,
                s.getObra() != null ? s.getObra().getTotalCapitulosLancados() : null,
                s.getObra() != null ? s.getObra().getTipoObra() : null,
                s.getUsuarioSugeridor() != null ? s.getUsuarioSugeridor().getId() : null,
                s.getUsuarioSugeridor() != null ? s.getUsuarioSugeridor().getApelidoOuEmail() : "Usuário Desvinculado",
                s.getTituloPrincipalSugerido(),
                s.getTituloOriginalSugerido(),
                s.getCapaUrlSugerida(),
                s.getSinopseSugerida(),
                s.getTotalCapitulosSugerido(),
                s.getTipoObraSugerido(),
                s.getJustificativa(),
                s.getStatus(),
                s.getDataCriacao(),
                s.getDataAvaliacao(),
                s.getParecerAdmin()
        );
    }
}
