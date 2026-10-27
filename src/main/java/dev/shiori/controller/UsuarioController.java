package dev.shiori.controller;

import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import dev.shiori.dto.usuario.AlterarEmailRequest;
import dev.shiori.dto.usuario.AlterarSenhaRequest;
import dev.shiori.dto.usuario.AtualizarPerfilRequest;
import dev.shiori.dto.usuario.ConcluirOnboardingRequest;
import dev.shiori.dto.usuario.UsuarioPerfilPublicoResponse;
import dev.shiori.dto.usuario.UsuarioPerfilResponse;
import dev.shiori.infra.security.CurrentUser;
import dev.shiori.infra.security.UsuarioPrincipal;
import dev.shiori.service.UsuarioService;
import jakarta.validation.Valid;

@RestController
@RequestMapping({"/api/usuarios", "/api/v1/usuarios"})
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UsuarioPerfilResponse> uploadAvatar(
            @CurrentUser UsuarioPrincipal usuarioLogado,
            @RequestParam("file") MultipartFile file) {
        if (usuarioLogado == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UsuarioPerfilResponse perfilAtualizado = usuarioService.atualizarAvatar(usuarioLogado.getId(), file);
        return ResponseEntity.ok(perfilAtualizado);
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioPerfilResponse> obterMeuPerfil(@CurrentUser UsuarioPrincipal usuarioLogado) {
        if (usuarioLogado == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UsuarioPerfilResponse perfil = usuarioService.buscarPerfilPorId(usuarioLogado.getId());
        return ResponseEntity.ok(perfil);
    }

    @PutMapping("/me/onboarding")
    public ResponseEntity<UsuarioPerfilResponse> concluirOnboarding(
            @CurrentUser UsuarioPrincipal usuarioLogado,
            @Valid @RequestBody ConcluirOnboardingRequest request) {
        if (usuarioLogado == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UsuarioPerfilResponse perfil = usuarioService.concluirOnboarding(usuarioLogado.getId(), request);
        return ResponseEntity.ok(perfil);
    }

    @PutMapping("/me/preferencias-formatos")
    public ResponseEntity<UsuarioPerfilResponse> atualizarPreferenciasFormatos(
            @CurrentUser UsuarioPrincipal usuarioLogado,
            @Valid @RequestBody dev.shiori.dto.usuario.AtualizarFormatosPreferidosRequest request) {
        if (usuarioLogado == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UsuarioPerfilResponse perfil = usuarioService.atualizarFormatosPreferidos(usuarioLogado.getId(), request.formatosPreferidos());
        return ResponseEntity.ok(perfil);
    }

    @PutMapping("/me/perfil")
    public ResponseEntity<UsuarioPerfilResponse> atualizarPerfil(
            @CurrentUser UsuarioPrincipal usuarioLogado,
            @Valid @RequestBody AtualizarPerfilRequest request) {
        if (usuarioLogado == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UsuarioPerfilResponse perfil = usuarioService.atualizarPerfil(usuarioLogado.getId(), request);
        return ResponseEntity.ok(perfil);
    }

    @PutMapping("/me/email")
    public ResponseEntity<UsuarioPerfilResponse> alterarEmail(
            @CurrentUser UsuarioPrincipal usuarioLogado,
            @Valid @RequestBody AlterarEmailRequest request) {
        if (usuarioLogado == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UsuarioPerfilResponse perfil = usuarioService.alterarEmail(usuarioLogado.getId(), request);
        return ResponseEntity.ok(perfil);
    }

    @PutMapping("/me/senha")
    public ResponseEntity<Void> alterarSenha(
            @CurrentUser UsuarioPrincipal usuarioLogado,
            @Valid @RequestBody AlterarSenhaRequest request) {
        if (usuarioLogado == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        usuarioService.alterarSenha(usuarioLogado.getId(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/me/exportar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> exportarDados(@CurrentUser UsuarioPrincipal usuarioLogado) {
        if (usuarioLogado == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, Object> dados = usuarioService.exportarDados(usuarioLogado.getId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"shiori_dados_usuario.json\"")
                .body(dados);
    }

    @GetMapping("/{username}/perfil")
    public ResponseEntity<UsuarioPerfilPublicoResponse> obterPerfilVisivel(
            @CurrentUser UsuarioPrincipal usuarioLogado,
            @PathVariable String username,
            @PageableDefault(size = 20) Pageable pageable) {
        Long solicitanteId = usuarioLogado != null ? usuarioLogado.getId() : null;
        UsuarioPerfilPublicoResponse response = usuarioService.obterPerfilVisivel(username, solicitanteId, pageable);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> excluirContaLGPD(@CurrentUser UsuarioPrincipal usuarioLogado) {
        if (usuarioLogado == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        usuarioService.anonimizarContaLGPD(usuarioLogado.getId());
        return ResponseEntity.noContent().build();
    }
}
