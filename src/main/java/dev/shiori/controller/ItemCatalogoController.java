package dev.shiori.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.shiori.dto.catalogo.AdicionarItemCatalogoRequest;
import dev.shiori.dto.catalogo.AtualizarVisibilidadeRequest;
import dev.shiori.dto.catalogo.ItemCatalogoResponse;
import dev.shiori.dto.catalogo.RegistrarProgressoRequest;
import dev.shiori.infra.security.CurrentUser;
import dev.shiori.infra.security.UsuarioPrincipal;
import dev.shiori.service.ItemCatalogoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping({"/api/catalogo", "/api/v1/catalogo"})
public class ItemCatalogoController {

    private final ItemCatalogoService itemCatalogoService;

    public ItemCatalogoController(ItemCatalogoService itemCatalogoService) {
        this.itemCatalogoService = itemCatalogoService;
    }

    @PostMapping
    public ResponseEntity<ItemCatalogoResponse> adicionarObra(
            @CurrentUser UsuarioPrincipal usuarioLogado,
            @Valid @RequestBody AdicionarItemCatalogoRequest request) {
        ItemCatalogoResponse response = itemCatalogoService.adicionarObraEstante(usuarioLogado.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/progresso")
    public ResponseEntity<ItemCatalogoResponse> registrarProgresso(
            @CurrentUser UsuarioPrincipal usuarioLogado,
            @PathVariable Long id,
            @Valid @RequestBody RegistrarProgressoRequest request) {
        ItemCatalogoResponse response = itemCatalogoService.registrarProgresso(usuarioLogado.getId(), id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/visibilidade")
    public ResponseEntity<ItemCatalogoResponse> atualizarVisibilidade(
            @CurrentUser UsuarioPrincipal usuarioLogado,
            @PathVariable Long id,
            @Valid @RequestBody AtualizarVisibilidadeRequest request) {
        ItemCatalogoResponse response = itemCatalogoService.alterarVisibilidade(usuarioLogado.getId(), id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemCatalogoResponse> buscarPorId(
            @CurrentUser UsuarioPrincipal usuarioLogado,
            @PathVariable Long id) {
        ItemCatalogoResponse response = itemCatalogoService.buscarPorId(usuarioLogado.getId(), id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/minha-estante")
    public ResponseEntity<Page<ItemCatalogoResponse>> listarMinhaEstante(
            @CurrentUser UsuarioPrincipal usuarioLogado,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ItemCatalogoResponse> response = itemCatalogoService.listarMinhaEstante(usuarioLogado.getId(), pageable);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removerItem(
            @CurrentUser UsuarioPrincipal usuarioLogado,
            @PathVariable Long id) {
        itemCatalogoService.removerItem(usuarioLogado.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/desvincular")
    public ResponseEntity<ItemCatalogoResponse> desvincularObra(
            @CurrentUser UsuarioPrincipal usuarioLogado,
            @PathVariable Long id) {
        ItemCatalogoResponse response = itemCatalogoService.desvincularObra(usuarioLogado.getId(), id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/vincular")
    public ResponseEntity<ItemCatalogoResponse> vincularObra(
            @CurrentUser UsuarioPrincipal usuarioLogado,
            @PathVariable Long id,
            @Valid @RequestBody dev.shiori.dto.catalogo.VincularObraRequest request) {
        ItemCatalogoResponse response = itemCatalogoService.vincularObra(
                usuarioLogado.getId(), id, request.obraId(), request.sincronizarOuPadrao());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/atualizar-meta")
    public ResponseEntity<ItemCatalogoResponse> atualizarMetaCapitulos(
            @CurrentUser UsuarioPrincipal usuarioLogado,
            @PathVariable Long id) {
        ItemCatalogoResponse response = itemCatalogoService.atualizarMetaNovosCapitulos(usuarioLogado.getId(), id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/exportar")
    public ResponseEntity<dev.shiori.dto.catalogo.EstanteExportDTO> exportarEstante(
            @CurrentUser UsuarioPrincipal usuarioLogado) {
        dev.shiori.dto.catalogo.EstanteExportDTO response = itemCatalogoService.exportarEstante(usuarioLogado.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/importar")
    public ResponseEntity<java.util.Map<String, Object>> importarEstante(
            @CurrentUser UsuarioPrincipal usuarioLogado,
            @Valid @RequestBody dev.shiori.dto.catalogo.EstanteImportRequest request) {
        int totalImportados = itemCatalogoService.importarEstante(usuarioLogado.getId(), request);
        return ResponseEntity.ok(java.util.Map.of("mensagem", "Estante importada com sucesso", "totalImportados", totalImportados));
    }
}
