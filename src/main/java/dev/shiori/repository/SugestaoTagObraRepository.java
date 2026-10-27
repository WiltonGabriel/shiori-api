package dev.shiori.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.shiori.domain.enums.StatusSugestaoTag;
import dev.shiori.domain.model.SugestaoTagObra;

public interface SugestaoTagObraRepository extends JpaRepository<SugestaoTagObra, Long> {

    @EntityGraph(attributePaths = {"obra", "tag", "usuarioSugeridor", "adminAvaliador"})
    Page<SugestaoTagObra> findByStatusOrderByDataSugestaoDesc(StatusSugestaoTag status, Pageable pageable);

    @EntityGraph(attributePaths = {"obra", "tag", "usuarioSugeridor", "adminAvaliador"})
    Page<SugestaoTagObra> findByObraIdOrderByDataSugestaoDesc(Long obraId, Pageable pageable);

    @EntityGraph(attributePaths = {"obra", "tag", "usuarioSugeridor", "adminAvaliador"})
    Page<SugestaoTagObra> findByUsuarioSugeridorIdOrderByDataSugestaoDesc(Long usuarioId, Pageable pageable);

    @EntityGraph(attributePaths = {"obra", "tag", "usuarioSugeridor", "adminAvaliador"})
    Optional<SugestaoTagObra> findById(Long id);

    boolean existsByObraIdAndNomeTagIgnoreCaseAndStatus(Long obraId, String nomeTag, StatusSugestaoTag status);

    boolean existsByObraIdAndTagIdAndStatus(Long obraId, Long tagId, StatusSugestaoTag status);

    @Query("SELECT COUNT(s) > 0 FROM SugestaoTagObra s LEFT JOIN s.tag t WHERE s.obra.id = :obraId AND s.status = :status AND (LOWER(s.nomeTag) = LOWER(:nomeTag) OR (t IS NOT NULL AND (LOWER(t.slug) = LOWER(:slug) OR LOWER(t.nome) = LOWER(:nomeTag))))")
    boolean existeSugestaoPendenteSimilar(
            @Param("obraId") Long obraId,
            @Param("nomeTag") String nomeTag,
            @Param("slug") String slug,
            @Param("status") StatusSugestaoTag status);

    long countByUsuarioSugeridorIdAndStatus(Long usuarioId, StatusSugestaoTag status);
}
