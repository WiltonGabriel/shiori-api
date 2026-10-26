package dev.shiori.domain.enums;

/**
 * Modo de avaliação lógica para filtros de coleções (gêneros e tags) na busca avançada.
 */
public enum ModoFiltroColecao {
    /**
     * Exige que a obra contenha TODOS os elementos especificados (interseção estrita).
     */
    AND,

    /**
     * Exige que a obra contenha PELO MENOS UM dos elementos especificados (união).
     */
    OR
}
