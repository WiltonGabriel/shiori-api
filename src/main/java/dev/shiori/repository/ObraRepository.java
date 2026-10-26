package dev.shiori.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import dev.shiori.domain.enums.StatusCatalogo;
import dev.shiori.domain.enums.TipoObra;
import dev.shiori.domain.model.Obra;

public interface ObraRepository extends JpaRepository<Obra, Long>, JpaSpecificationExecutor<Obra> {

    @Override
    @EntityGraph(attributePaths = {"generos", "tags"})
    List<Obra> findAll();

    @Override
    @EntityGraph(attributePaths = {"generos", "tags"})
    Optional<Obra> findById(Long id);

    @EntityGraph(attributePaths = {"generos", "tags"})
    List<Obra> findByStatusCatalogo(StatusCatalogo statusCatalogo);

    @EntityGraph(attributePaths = {"generos", "tags"})
    List<Obra> findByStatusCatalogoIn(Collection<StatusCatalogo> statusCatalogos);

    @EntityGraph(attributePaths = {"generos", "tags"})
    List<Obra> findByTipoObra(TipoObra tipoObra);

    @EntityGraph(attributePaths = {"generos", "tags"})
    List<Obra> findByTagsContains(dev.shiori.domain.model.Tag tag);

    @EntityGraph(attributePaths = {"generos", "tags"})
    @org.springframework.data.jpa.repository.Query("SELECT o FROM Obra o WHERE LOWER(o.tituloPrincipal) LIKE LOWER(CONCAT('%', :termo, '%')) OR (o.tituloOriginal IS NOT NULL AND LOWER(o.tituloOriginal) LIKE LOWER(CONCAT('%', :termo, '%')))")
    List<Obra> pesquisarPorTermo(@org.springframework.data.repository.query.Param("termo") String termo);

    Optional<Obra> findByMangadexId(String mangadexId);

    Optional<Obra> findByAnilistId(String anilistId);

    Optional<Obra> findByKitsuId(String kitsuId);

    Optional<Obra> findByIsbn(String isbn);

    Optional<Obra> findByTituloPrincipalIgnoreCase(String tituloPrincipal);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Obra o SET o.criadoPorUsuarioId = null WHERE o.criadoPorUsuarioId = :usuarioId")
    void desvincularCriador(@org.springframework.data.repository.query.Param("usuarioId") Long usuarioId);
}