package dev.shiori.domain.enums;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Taxonomia oficial de formatos literários suportados pelo ecossistema Shiori:
 * MANGA, MANHWA, MANHUA, LIGHT_NOVEL, WEB_NOVEL, COMIC, LIVRO.
 * Mantém NOVEL deprecado para tolerância a dados legados em banco e APIs.
 */
public enum TipoObra {
    MANGA("Mangá"),
    MANHWA("Manhwa"),
    MANHUA("Manhua"),
    LIGHT_NOVEL("Light Novel"),
    WEB_NOVEL("Web Novel"),
    COMIC("Comic"),
    LIVRO("Livro"),

    /**
     * @deprecated Formato legado mantido para retrocompatibilidade com registros existentes no banco.
     */
    @Deprecated
    NOVEL("Light Novel (Legado)");

    private final String descricao;

    TipoObra(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public static final Set<TipoObra> FORMATOS_OFICIAIS = Set.of(
            MANGA, MANHWA, MANHUA, LIGHT_NOVEL, WEB_NOVEL, COMIC, LIVRO
    );

    @JsonCreator
    public static TipoObra fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalizado = value.trim().toUpperCase();
        if ("NOVEL".equals(normalizado)) {
            return LIGHT_NOVEL;
        }
        return TipoObra.valueOf(normalizado);
    }
}
