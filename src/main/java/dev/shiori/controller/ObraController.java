package dev.shiori.controller;

import java.net.URI;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import dev.shiori.domain.enums.StatusCatalogo;
import dev.shiori.domain.enums.TipoObra;
import dev.shiori.dto.ObraCadastroRequest;
import dev.shiori.dto.ObraResponse;
import dev.shiori.dto.ResultadoBuscaObrasResponse;
import dev.shiori.dto.obra.ObraBuscaAvancadaFiltro;
import dev.shiori.infra.integration.IntegracaoMetadadosObra;
import dev.shiori.infra.security.CurrentUser;
import dev.shiori.infra.security.UsuarioPrincipal;
import dev.shiori.service.IntegracaoCatalogoService;
import dev.shiori.service.ObraService;
import dev.shiori.service.UsuarioService;
import jakarta.validation.Valid;

@RestController
@RequestMapping({"/obras", "/api/obras", "/api/v1/obras"})
public class ObraController {

    private final ObraService obraService;
    private final IntegracaoCatalogoService integracaoCatalogoService;
    private final UsuarioService usuarioService;

    public ObraController(ObraService obraService,
                          IntegracaoCatalogoService integracaoCatalogoService,
                          UsuarioService usuarioService) {
        this.obraService = obraService;
        this.integracaoCatalogoService = integracaoCatalogoService;
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<ObraResponse> cadastrar(
            @RequestBody @Valid ObraCadastroRequest dados,
            UriComponentsBuilder uriBuilder) {

        ObraResponse response = obraService.cadastrarObra(dados);
        URI uri = uriBuilder.path("/obras/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @PutMapping("/{id}/submeter")
    public ResponseEntity<ObraResponse> submeterAoCatalogo(
            @PathVariable Long id,
            @RequestParam Long usuarioId) {

        ObraResponse response = obraService.submeterAoCatalogoGlobal(id, usuarioId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/promover")
    public ResponseEntity<ObraResponse> promoverParaOficial(@PathVariable Long id) {
        ObraResponse response = obraService.promoverParaOficial(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ObraResponse>> listarTodas(
            @RequestParam(required = false) StatusCatalogo status,
            @CurrentUser UsuarioPrincipal usuarioLogado) {

        Long usuarioId = usuarioLogado != null ? usuarioLogado.getId() : null;
        List<ObraResponse> lista = obraService.listarTodas(status, usuarioId);
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/home")
    public ResponseEntity<List<ObraResponse>> listarObrasHome(@CurrentUser UsuarioPrincipal usuarioLogado) {
        Long usuarioId = usuarioLogado != null ? usuarioLogado.getId() : null;
        List<ObraResponse> lista = obraService.listarObrasDaHome(usuarioId);
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/busca")
    public ResponseEntity<ResultadoBuscaObrasResponse> buscarObrasAssistida(
            @RequestParam String termo,
            @CurrentUser UsuarioPrincipal usuarioLogado) {

        Long usuarioId = usuarioLogado != null ? usuarioLogado.getId() : null;
        ResultadoBuscaObrasResponse response = obraService.buscarObrasComAssistencia(termo, usuarioId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/busca-avancada")
    public ResponseEntity<Page<ObraResponse>> buscarObrasAvancada(
            @Valid @ModelAttribute ObraBuscaAvancadaFiltro filtro) {
        Page<ObraResponse> pagina = obraService.buscarObrasAvancada(filtro);
        return ResponseEntity.ok(pagina);
    }

    @GetMapping("/externas/pesquisar")
    public ResponseEntity<List<IntegracaoMetadadosObra>> pesquisarEmProvedoresExternos(
            @RequestParam String termo,
            @RequestParam(required = false) Integer limite,
            @CurrentUser UsuarioPrincipal usuarioLogado) {

        Set<TipoObra> formatosPermitidos = null;
        if (usuarioLogado != null && usuarioLogado.getId() != null) {
            formatosPermitidos = usuarioService.buscarPerfilPorId(usuarioLogado.getId()).formatosPreferidos();
        }

        List<IntegracaoMetadadosObra> resultados = integracaoCatalogoService.pesquisarEmProvedoresExternos(
                termo,
                limite,
                formatosPermitidos
        );
        return ResponseEntity.ok(resultados);
    }

    @GetMapping("/externas/{provedor}/obras/{idExterno}")
    public ResponseEntity<IntegracaoMetadadosObra> consultarObraExterna(
            @PathVariable String provedor,
            @PathVariable String idExterno) {

        IntegracaoMetadadosObra response = integracaoCatalogoService.consultarObraPorIdExterno(provedor, idExterno);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ObraResponse> buscarPorId(@PathVariable Long id) {
        ObraResponse response = obraService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/sugestoes-edicao")
    public ResponseEntity<dev.shiori.dto.admin.SugestaoEdicaoResponse> sugerirEdicao(
            @PathVariable Long id,
            @jakarta.validation.Valid @RequestBody dev.shiori.dto.SugestaoEdicaoRequest request,
            @dev.shiori.infra.security.CurrentUser dev.shiori.infra.security.UsuarioPrincipal usuarioLogado) {
        dev.shiori.dto.admin.SugestaoEdicaoResponse response = obraService.sugerirEdicao(id, request, usuarioLogado.getId());
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        obraService.excluirObra(id);
        return ResponseEntity.noContent().build();
    }
}
