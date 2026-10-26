package dev.shiori.repository.specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import dev.shiori.domain.enums.ModoFiltroColecao;
import dev.shiori.domain.enums.StatusCatalogo;
import dev.shiori.domain.model.Genero;
import dev.shiori.domain.model.Obra;
import dev.shiori.domain.model.Tag;
import dev.shiori.dto.obra.ObraBuscaAvancadaFiltro;
import dev.shiori.dto.obra.OrdenacaoObra;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Construtor dinâmico de consultas para a busca avançada do catálogo (Criteria API).
 * Utiliza subconsultas correlacionadas com EXISTS/NOT EXISTS para garantir que junções
 * ManyToMany (gêneros e tags) não multipliquem linhas ou quebrem a paginação SQL.
 */
public final class ObraSpecification {

    private ObraSpecification() {
    }

    public static Specification<Obra> construir(ObraBuscaAvancadaFiltro filtro) {
        return (Root<Obra> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Busca textual no título principal ou título original
            if (filtro.termo() != null && !filtro.termo().trim().isBlank()) {
                String pattern = "%" + filtro.termo().trim().toLowerCase() + "%";
                Predicate matchPrincipal = builder.like(builder.lower(root.get("tituloPrincipal")), pattern);
                Predicate matchOriginal = builder.and(
                        builder.isNotNull(root.get("tituloOriginal")),
                        builder.like(builder.lower(root.get("tituloOriginal")), pattern)
                );
                predicates.add(builder.or(matchPrincipal, matchOriginal));
            }

            // 2. Filtro de Tipos / Formatos
            if (filtro.tipos() != null && !filtro.tipos().isEmpty()) {
                predicates.add(root.get("tipoObra").in(filtro.tipos()));
            }

            // 3. Filtro de Status de Publicação
            if (filtro.status() != null && !filtro.status().isEmpty()) {
                predicates.add(root.get("statusPublicacao").in(filtro.status()));
            }

            // 4. Status de Catálogo (Default defensivo: OFICIAL e COMUNITARIO)
            if (filtro.statusCatalogo() != null && !filtro.statusCatalogo().isEmpty()) {
                predicates.add(root.get("statusCatalogo").in(filtro.statusCatalogo()));
            } else {
                predicates.add(root.get("statusCatalogo").in(Set.of(StatusCatalogo.OFICIAL, StatusCatalogo.COMUNITARIO)));
            }

            // 5. Intervalo de Capítulos
            if (filtro.minCapitulos() != null) {
                predicates.add(builder.and(
                        builder.isNotNull(root.get("totalCapitulosLancados")),
                        builder.ge(root.get("totalCapitulosLancados"), filtro.minCapitulos())
                ));
            }
            if (filtro.maxCapitulos() != null) {
                predicates.add(builder.and(
                        builder.isNotNull(root.get("totalCapitulosLancados")),
                        builder.le(root.get("totalCapitulosLancados"), filtro.maxCapitulos())
                ));
            }

            // 6. Intervalo de Nota
            if (filtro.minNota() != null) {
                predicates.add(builder.and(
                        builder.isNotNull(root.get("notaGeral")),
                        builder.ge(root.get("notaGeral"), filtro.minNota())
                ));
            }
            if (filtro.maxNota() != null) {
                predicates.add(builder.and(
                        builder.isNotNull(root.get("notaGeral")),
                        builder.le(root.get("notaGeral"), filtro.maxNota())
                ));
            }

            // 7. Gêneros Incluídos (AND vs OR)
            if (filtro.generosIncluidos() != null && !filtro.generosIncluidos().isEmpty()) {
                if (filtro.generosModo() == ModoFiltroColecao.AND) {
                    for (String generoTermo : filtro.generosIncluidos()) {
                        if (generoTermo != null && !generoTermo.trim().isBlank()) {
                            predicates.add(builder.exists(criarSubqueryGenero(root, query, builder, generoTermo.trim())));
                        }
                    }
                } else {
                    List<Predicate> orPredicates = new ArrayList<>();
                    for (String generoTermo : filtro.generosIncluidos()) {
                        if (generoTermo != null && !generoTermo.trim().isBlank()) {
                            orPredicates.add(builder.exists(criarSubqueryGenero(root, query, builder, generoTermo.trim())));
                        }
                    }
                    if (!orPredicates.isEmpty()) {
                        predicates.add(builder.or(orPredicates.toArray(new Predicate[0])));
                    }
                }
            }

            // 8. Gêneros Excluídos (NOT)
            if (filtro.generosExcluidos() != null && !filtro.generosExcluidos().isEmpty()) {
                for (String generoTermo : filtro.generosExcluidos()) {
                    if (generoTermo != null && !generoTermo.trim().isBlank()) {
                        predicates.add(builder.not(builder.exists(criarSubqueryGenero(root, query, builder, generoTermo.trim()))));
                    }
                }
            }

            // 9. Tags Incluídas (AND vs OR)
            if (filtro.tagsIncluidas() != null && !filtro.tagsIncluidas().isEmpty()) {
                if (filtro.tagsModo() == ModoFiltroColecao.AND) {
                    for (String tagTermo : filtro.tagsIncluidas()) {
                        if (tagTermo != null && !tagTermo.trim().isBlank()) {
                            predicates.add(builder.exists(criarSubqueryTag(root, query, builder, tagTermo.trim())));
                        }
                    }
                } else {
                    List<Predicate> orPredicates = new ArrayList<>();
                    for (String tagTermo : filtro.tagsIncluidas()) {
                        if (tagTermo != null && !tagTermo.trim().isBlank()) {
                            orPredicates.add(builder.exists(criarSubqueryTag(root, query, builder, tagTermo.trim())));
                        }
                    }
                    if (!orPredicates.isEmpty()) {
                        predicates.add(builder.or(orPredicates.toArray(new Predicate[0])));
                    }
                }
            }

            // 10. Tags Excluídas (NOT)
            if (filtro.tagsExcluidas() != null && !filtro.tagsExcluidas().isEmpty()) {
                for (String tagTermo : filtro.tagsExcluidas()) {
                    if (tagTermo != null && !tagTermo.trim().isBlank()) {
                        predicates.add(builder.not(builder.exists(criarSubqueryTag(root, query, builder, tagTermo.trim()))));
                    }
                }
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Sort construirSort(OrdenacaoObra ordenacao) {
        if (ordenacao == null) {
            ordenacao = OrdenacaoObra.NOTA_DESC;
        }
        return switch (ordenacao) {
            case NOTA_DESC -> Sort.by(Sort.Order.desc("notaGeral").nullsLast(), Sort.Order.desc("id"));
            case NOTA_ASC -> Sort.by(Sort.Order.asc("notaGeral").nullsLast(), Sort.Order.asc("id"));
            case CAPITULOS_DESC -> Sort.by(Sort.Order.desc("totalCapitulosLancados").nullsLast(), Sort.Order.desc("id"));
            case CAPITULOS_ASC -> Sort.by(Sort.Order.asc("totalCapitulosLancados").nullsLast(), Sort.Order.asc("id"));
            case TITULO_ASC -> Sort.by(Sort.Order.asc("tituloPrincipal"), Sort.Order.asc("id"));
            case TITULO_DESC -> Sort.by(Sort.Order.desc("tituloPrincipal"), Sort.Order.desc("id"));
            case RECENTES -> Sort.by(Sort.Order.desc("id"));
            case ANTIGAS -> Sort.by(Sort.Order.asc("id"));
        };
    }

    private static Subquery<Integer> criarSubqueryGenero(Root<Obra> root, CriteriaQuery<?> query, CriteriaBuilder builder, String termo) {
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<Obra> subObra = subquery.from(Obra.class);
        Join<Obra, Genero> joinGenero = subObra.join("generos");
        String termoLower = termo.toLowerCase();

        subquery.select(builder.literal(1))
                .where(
                        builder.equal(subObra.get("id"), root.get("id")),
                        builder.or(
                                builder.equal(builder.lower(joinGenero.get("slug")), termoLower),
                                builder.equal(builder.lower(joinGenero.get("nome")), termoLower)
                        )
                );
        return subquery;
    }

    private static Subquery<Integer> criarSubqueryTag(Root<Obra> root, CriteriaQuery<?> query, CriteriaBuilder builder, String termo) {
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<Obra> subObra = subquery.from(Obra.class);
        Join<Obra, Tag> joinTag = subObra.join("tags");
        String termoLower = termo.toLowerCase();

        subquery.select(builder.literal(1))
                .where(
                        builder.equal(subObra.get("id"), root.get("id")),
                        builder.or(
                                builder.equal(builder.lower(joinTag.get("slug")), termoLower),
                                builder.equal(builder.lower(joinTag.get("nome")), termoLower)
                        )
                );
        return subquery;
    }
}
