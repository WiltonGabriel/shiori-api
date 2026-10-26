package dev.shiori.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import dev.shiori.dto.tag.SugestaoTagResponse;
import dev.shiori.dto.tag.SugerirTagRequest;
import dev.shiori.dto.tag.TagObraResponse;
import dev.shiori.dto.tag.TagResponse;
import dev.shiori.dto.tag.VotoTagRequest;
import dev.shiori.dto.tag.VotoTagResponse;
import dev.shiori.infra.security.CurrentUser;
import dev.shiori.infra.security.UsuarioPrincipal;
import dev.shiori.service.TagService;
import jakarta.validation.Valid;

@RestController
@RequestMapping
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @PostMapping({"/obras/{obraId}/tags/sugestoes", "/api/obras/{obraId}/tags/sugestoes"})
    public ResponseEntity<SugestaoTagResponse> sugerirTag(
            @PathVariable Long obraId,
            @RequestBody @Valid SugerirTagRequest request,
            @CurrentUser UsuarioPrincipal usuarioLogado,
            UriComponentsBuilder uriBuilder) {

        Long usuarioId = usuarioLogado != null ? usuarioLogado.getId() : null;
        SugestaoTagResponse response = tagService.sugerirTagParaObra(obraId, usuarioId, request);

        URI uri = uriBuilder.path("/api/obras/{obraId}/tags/sugestoes/{id}")
                .buildAndExpand(obraId, response.id())
                .toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @GetMapping({"/obras/{obraId}/tags", "/api/obras/{obraId}/tags"})
    public ResponseEntity<List<TagObraResponse>> listarTagsDaObra(
            @PathVariable Long obraId,
            @CurrentUser UsuarioPrincipal usuarioLogado) {

        Long usuarioId = usuarioLogado != null ? usuarioLogado.getId() : null;
        List<TagObraResponse> tags = tagService.listarTagsDaObra(obraId, usuarioId);
        return ResponseEntity.ok(tags);
    }

    @PostMapping({"/obras/{obraId}/tags/{tagId}/votar", "/api/obras/{obraId}/tags/{tagId}/votar"})
    public ResponseEntity<VotoTagResponse> votarEmTag(
            @PathVariable Long obraId,
            @PathVariable Long tagId,
            @RequestBody @Valid VotoTagRequest request,
            @CurrentUser UsuarioPrincipal usuarioLogado) {

        Long usuarioId = usuarioLogado != null ? usuarioLogado.getId() : null;
        VotoTagResponse response = tagService.votarEmTag(obraId, tagId, usuarioId, request.tipoVoto());
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/tags", "/api/tags"})
    public ResponseEntity<List<TagResponse>> pesquisarTags(
            @RequestParam(required = false) String termo,
            @RequestParam(required = false) Integer limite) {

        List<TagResponse> tags = tagService.pesquisarTags(termo, limite);
        return ResponseEntity.ok(tags);
    }
}
