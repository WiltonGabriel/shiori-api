package dev.shiori.dto.obra;

import java.util.Set;

import dev.shiori.domain.enums.ModoFiltroColecao;
import dev.shiori.domain.enums.StatusCatalogo;
import dev.shiori.domain.enums.StatusPublicacao;
import dev.shiori.domain.enums.TipoObra;
import dev.shiori.domain.exception.RegraDeNegocioException;

/**
 * Objeto de parâmetros (DTO imutável) para a busca avançada paginada no catálogo global.
 * Suporta lógica combinada de formatos, status, faixas numéricas de capítulos/notas e coleções com AND/OR/NOT.
 */
public record ObraBuscaAvancadaFiltro(
        String termo,
        Set<TipoObra> tipos,
        Set<StatusPublicacao> status,
        Set<StatusCatalogo> statusCatalogo,
        Integer minCapitulos,
        Integer maxCapitulos,
        Double minNota,
        Double maxNota,
        Set<String> generosIncluidos,
        ModoFiltroColecao generosModo,
        Set<String> generosExcluidos,
        Set<String> tagsIncluidas,
        ModoFiltroColecao tagsModo,
        Set<String> tagsExcluidas,
        OrdenacaoObra ordenacao,
        Integer pagina,
        Integer itensPorPagina
) {
    public ObraBuscaAvancadaFiltro {
        if (generosModo == null) {
            generosModo = ModoFiltroColecao.AND;
        }
        if (tagsModo == null) {
            tagsModo = ModoFiltroColecao.AND;
        }
        if (ordenacao == null) {
            ordenacao = OrdenacaoObra.NOTA_DESC;
        }
        if (pagina == null || pagina < 0) {
            pagina = 0;
        }
        if (itensPorPagina == null || itensPorPagina < 1) {
            itensPorPagina = 20;
        } else if (itensPorPagina > 50) {
            itensPorPagina = 50;
        }
    }

    /**
     * Validação estrita das invariantes de negócio e limites numéricos (Fail-Fast).
     */
    public void validar() {
        if (minCapitulos != null && minCapitulos < 0) {
            throw new RegraDeNegocioException("O número mínimo de capítulos não pode ser negativo");
        }
        if (maxCapitulos != null && maxCapitulos < 0) {
            throw new RegraDeNegocioException("O número máximo de capítulos não pode ser negativo");
        }
        if (minCapitulos != null && maxCapitulos != null && minCapitulos > maxCapitulos) {
            throw new RegraDeNegocioException("O número mínimo de capítulos não pode ser maior que o máximo");
        }
        if (minNota != null && (minNota < 0.0 || minNota > 10.0)) {
            throw new RegraDeNegocioException("A nota mínima deve estar entre 0.0 e 10.0");
        }
        if (maxNota != null && (maxNota < 0.0 || maxNota > 10.0)) {
            throw new RegraDeNegocioException("A nota máxima deve estar entre 0.0 e 10.0");
        }
        if (minNota != null && maxNota != null && minNota > maxNota) {
            throw new RegraDeNegocioException("A nota mínima não pode ser maior que a nota máxima");
        }
    }
}
