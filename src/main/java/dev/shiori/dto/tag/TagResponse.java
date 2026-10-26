package dev.shiori.dto.tag;

import dev.shiori.domain.model.Tag;

public record TagResponse(
        Long id,
        String nome,
        String slug
) {
    public static TagResponse fromEntity(Tag tag) {
        return new TagResponse(tag.getId(), tag.getNome(), tag.getSlug());
    }
}
