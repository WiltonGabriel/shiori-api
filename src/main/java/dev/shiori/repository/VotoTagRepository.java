package dev.shiori.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.shiori.domain.model.VotoTag;

public interface VotoTagRepository extends JpaRepository<VotoTag, Long> {

    List<VotoTag> findByTagId(Long tagId);

    List<VotoTag> findByObraId(Long obraId);

    List<VotoTag> findByObraIdAndTagId(Long obraId, Long tagId);

    Optional<VotoTag> findByUsuarioIdAndTagIdAndObraId(Long usuarioId, Long tagId, Long obraId);

    @Modifying
    @Query("DELETE FROM VotoTag v WHERE v.tag.id = :tagId")
    void deletarPorTagId(@Param("tagId") Long tagId);
}
