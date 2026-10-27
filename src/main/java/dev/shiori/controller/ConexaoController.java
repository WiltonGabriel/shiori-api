package dev.shiori.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.shiori.dto.conexao.ConexaoResponse;
import dev.shiori.infra.security.CurrentUser;
import dev.shiori.infra.security.UsuarioPrincipal;
import dev.shiori.service.ConexaoUsuarioService;

@RestController
@RequestMapping({"/api/conexoes", "/api/v1/conexoes"})
public class ConexaoController {

    private final ConexaoUsuarioService conexaoUsuarioService;

    public ConexaoController(ConexaoUsuarioService conexaoUsuarioService) {
        this.conexaoUsuarioService = conexaoUsuarioService;
    }

    @PostMapping("/solicitar/{usuarioId}")
    public ResponseEntity<ConexaoResponse> solicitarConexao(
            @CurrentUser UsuarioPrincipal usuarioLogado,
            @PathVariable Long usuarioId) {
        ConexaoResponse response = conexaoUsuarioService.solicitarConexao(usuarioLogado.getId(), usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{conexaoId}/aceitar")
    public ResponseEntity<ConexaoResponse> aceitarConexao(
            @CurrentUser UsuarioPrincipal usuarioLogado,
            @PathVariable Long conexaoId) {
        ConexaoResponse response = conexaoUsuarioService.aceitarConexao(usuarioLogado.getId(), conexaoId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{conexaoId}/bloquear")
    public ResponseEntity<ConexaoResponse> bloquearConexao(
            @CurrentUser UsuarioPrincipal usuarioLogado,
            @PathVariable Long conexaoId) {
        ConexaoResponse response = conexaoUsuarioService.bloquearConexao(usuarioLogado.getId(), conexaoId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pendentes")
    public ResponseEntity<Page<ConexaoResponse>> listarSolicitacoesPendentes(
            @CurrentUser UsuarioPrincipal usuarioLogado,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ConexaoResponse> pendentes = conexaoUsuarioService.listarSolicitacoesPendentes(usuarioLogado.getId(), pageable);
        return ResponseEntity.ok(pendentes);
    }

    @GetMapping
    public ResponseEntity<List<ConexaoResponse>> listarConexoesAceitas(
            @CurrentUser UsuarioPrincipal usuarioLogado) {
        List<ConexaoResponse> conexoes = conexaoUsuarioService.listarConexoesAceitas(usuarioLogado.getId());
        return ResponseEntity.ok(conexoes);
    }
}
