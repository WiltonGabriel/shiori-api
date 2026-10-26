package dev.shiori.dto.obra;

/**
 * Opções de ordenação permitidas para a busca avançada do catálogo.
 * Cada opção possui amarração com chave secundária estável (ID) para evitar paginação inconsistente.
 */
public enum OrdenacaoObra {
    NOTA_DESC,
    NOTA_ASC,
    CAPITULOS_DESC,
    CAPITULOS_ASC,
    TITULO_ASC,
    TITULO_DESC,
    RECENTES,
    ANTIGAS
}
