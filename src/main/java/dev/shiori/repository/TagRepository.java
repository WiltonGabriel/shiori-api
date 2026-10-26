package dev.shiori.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import dev.shiori.domain.model.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByNomeIgnoreCase(String nome);

    Optional<Tag> findBySlugIgnoreCase(String slug);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsBySlugIgnoreCase(String slug);

    List<Tag> findByNomeContainingIgnoreCase(String termo);

    Page<Tag> findByNomeContainingIgnoreCase(String termo, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Tag t SET t.criadoPorUsuarioId = null WHERE t.criadoPorUsuarioId = :usuarioId")
    void desvincularCriador(@org.springframework.data.repository.query.Param("usuarioId") Long usuarioId);
}
