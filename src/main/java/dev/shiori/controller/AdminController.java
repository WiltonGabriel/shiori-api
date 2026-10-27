package dev.shiori.controller;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.shiori.dto.ObraResponse;
import dev.shiori.dto.admin.AdminDashboardResponse;
import dev.shiori.dto.admin.AdminUsuarioResponse;
import dev.shiori.dto.admin.AlterarFeatureFlagRequest;
import dev.shiori.dto.admin.AlterarRoleUsuarioRequest;
import dev.shiori.dto.admin.AlterarStatusUsuarioRequest;
import dev.shiori.dto.admin.AplicarStrikeRequest;
import dev.shiori.dto.admin.AvaliarSugestaoRequest;
import dev.shiori.dto.admin.BanirUsuarioRequest;
import dev.shiori.dto.admin.BroadcastResponse;
import dev.shiori.dto.admin.CriarBroadcastRequest;
import dev.shiori.dto.admin.CriarGeneroRequest;
import dev.shiori.dto.admin.DenunciaComentarioResponse;
import dev.shiori.dto.admin.DenunciaObraResponse;
import dev.shiori.dto.admin.FeatureFlagResponse;
import dev.shiori.dto.admin.GeneroResponse;
import dev.shiori.dto.admin.MesclarObrasRequest;
import dev.shiori.dto.admin.MesclarObrasResponse;
import dev.shiori.dto.admin.MesclarTagsRequest;
import dev.shiori.dto.admin.MesclarTagsResponse;
import dev.shiori.dto.admin.OverrideObraRequest;
import dev.shiori.dto.admin.ParecerModeracaoRequest;
import dev.shiori.dto.admin.ProvedorIntegracaoResponse;
import dev.shiori.dto.admin.RegistroAuditoriaResponse;
import dev.shiori.dto.admin.SalvarProvedorIntegracaoRequest;
import dev.shiori.dto.admin.SugestaoEdicaoResponse;
import dev.shiori.dto.tag.AvaliarSugestaoTagRequest;
import dev.shiori.dto.tag.SugestaoTagAdminResponse;
import dev.shiori.domain.enums.StatusSugestaoTag;
import dev.shiori.infra.integration.IntegracaoMetadadosObra;
import dev.shiori.infra.security.CurrentUser;
import dev.shiori.infra.security.UsuarioPrincipal;
import dev.shiori.service.AdminService;
import dev.shiori.service.TagService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'CURADOR')")
public class AdminController {

    private final AdminService adminService;
    private final TagService tagService;

    public AdminController(AdminService adminService, TagService tagService) {
        this.adminService = adminService;
        this.tagService = tagService;
    }

    // ==========================================
    // 1. DASHBOARD & MÉTRICAS
    // ==========================================

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> obterDashboard(@CurrentUser UsuarioPrincipal adminLogado) {
        AdminDashboardResponse response = adminService.obterDashboard(adminLogado.getId());
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 2. GESTÃO DE USUÁRIOS & STRIKES
    // ==========================================

    @GetMapping("/usuarios")
    public ResponseEntity<Page<AdminUsuarioResponse>> listarUsuarios(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AdminUsuarioResponse> usuarios = adminService.listarUsuarios(pageable, adminLogado.getId());
        return ResponseEntity.ok(usuarios);
    }

    @PutMapping("/usuarios/{usuarioId}/status")
    public ResponseEntity<AdminUsuarioResponse> alterarStatusUsuario(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PathVariable Long usuarioId,
            @Valid @RequestBody AlterarStatusUsuarioRequest request) {
        AdminUsuarioResponse response = adminService.alterarStatusUsuario(usuarioId, request, adminLogado.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/usuarios/{usuarioId}/role")
    public ResponseEntity<AdminUsuarioResponse> alterarRoleUsuario(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PathVariable Long usuarioId,
            @Valid @RequestBody AlterarRoleUsuarioRequest request) {
        AdminUsuarioResponse response = adminService.alterarRoleUsuario(usuarioId, request, adminLogado.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/usuarios/{usuarioId}/banir")
    public ResponseEntity<Void> banirUsuario(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PathVariable Long usuarioId,
            @Valid @RequestBody BanirUsuarioRequest request) {
        adminService.banirUsuario(usuarioId, request.motivo(), adminLogado.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/usuarios/{usuarioId}/strikes")
    public ResponseEntity<AdminUsuarioResponse> aplicarStrike(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PathVariable Long usuarioId,
            @Valid @RequestBody AplicarStrikeRequest request) {
        AdminUsuarioResponse response = adminService.aplicarStrikeUsuario(usuarioId, request, adminLogado.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/usuarios/{usuarioId}/strikes")
    public ResponseEntity<AdminUsuarioResponse> removerStrike(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PathVariable Long usuarioId,
            @RequestParam(defaultValue = "Revisão manual da moderação") String motivo) {
        AdminUsuarioResponse response = adminService.removerStrikeUsuario(usuarioId, motivo, adminLogado.getId());
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 3. CURADORIA DO CATÁLOGO & OVERRIDE
    // ==========================================

    @PostMapping("/obras/mesclar")
    public ResponseEntity<MesclarObrasResponse> mesclarObras(
            @CurrentUser UsuarioPrincipal adminLogado,
            @Valid @RequestBody MesclarObrasRequest request) {
        MesclarObrasResponse response = adminService.mesclarObras(request, adminLogado.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/obras/{id}/oficializar")
    public ResponseEntity<ObraResponse> oficializarObra(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PathVariable Long id) {
        ObraResponse response = adminService.oficializarObra(id, adminLogado.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/obras/{id}/override")
    public ResponseEntity<ObraResponse> aplicarOverrideObra(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PathVariable Long id,
            @Valid @RequestBody OverrideObraRequest request) {
        ObraResponse response = adminService.aplicarOverrideObra(id, request, adminLogado.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/generos")
    public ResponseEntity<List<GeneroResponse>> listarGeneros(@CurrentUser UsuarioPrincipal adminLogado) {
        List<GeneroResponse> lista = adminService.listarGeneros(adminLogado.getId());
        return ResponseEntity.ok(lista);
    }

    @PostMapping("/generos")
    public ResponseEntity<GeneroResponse> criarGenero(
            @CurrentUser UsuarioPrincipal adminLogado,
            @Valid @RequestBody CriarGeneroRequest request) {
        GeneroResponse response = adminService.criarGenero(request, adminLogado.getId());
        return ResponseEntity.created(URI.create("/api/generos/" + response.slug())).body(response);
    }

    @PostMapping("/tags/mesclar")
    public ResponseEntity<MesclarTagsResponse> mesclarTags(
            @CurrentUser UsuarioPrincipal adminLogado,
            @Valid @RequestBody MesclarTagsRequest request) {
        MesclarTagsResponse response = adminService.mesclarTags(request, adminLogado.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tags/sugestoes")
    public ResponseEntity<Page<SugestaoTagAdminResponse>> listarSugestoesTags(
            @CurrentUser UsuarioPrincipal adminLogado,
            @RequestParam(required = false) StatusSugestaoTag status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<SugestaoTagAdminResponse> sugestoes = tagService.listarSugestoes(status, pageable, adminLogado.getId());
        return ResponseEntity.ok(sugestoes);
    }

    @PostMapping("/tags/sugestoes/{id}/aprovar")
    public ResponseEntity<SugestaoTagAdminResponse> aprovarSugestaoTag(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PathVariable Long id) {
        SugestaoTagAdminResponse response = tagService.aprovarSugestao(id, adminLogado.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/tags/sugestoes/{id}/rejeitar")
    public ResponseEntity<SugestaoTagAdminResponse> rejeitarSugestaoTag(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PathVariable Long id,
            @Valid @RequestBody AvaliarSugestaoTagRequest request) {
        SugestaoTagAdminResponse response = tagService.rejeitarSugestao(id, adminLogado.getId(), request.motivo());
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 4. INTEGRAÇÕES EXTERNAS DINÂMICAS
    // ==========================================

    @GetMapping({"/integracoes", "/provedores"})
    public ResponseEntity<List<ProvedorIntegracaoResponse>> listarIntegracoes(@CurrentUser UsuarioPrincipal adminLogado) {
        List<ProvedorIntegracaoResponse> lista = adminService.listarProvedoresIntegracao(adminLogado.getId());
        return ResponseEntity.ok(lista);
    }

    @PostMapping({"/integracoes", "/provedores"})
    public ResponseEntity<ProvedorIntegracaoResponse> salvarIntegracao(
            @CurrentUser UsuarioPrincipal adminLogado,
            @Valid @RequestBody SalvarProvedorIntegracaoRequest request) {
        ProvedorIntegracaoResponse response = adminService.salvarProvedorIntegracao(request, adminLogado.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/integracoes/{id}/status")
    public ResponseEntity<ProvedorIntegracaoResponse> alternarStatusIntegracao(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PathVariable Long id,
            @RequestParam boolean ativo) {
        ProvedorIntegracaoResponse response = adminService.alternarStatusProvedor(id, ativo, adminLogado.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/integracoes/{codigo}/obras/{idExterno}")
    public ResponseEntity<IntegracaoMetadadosObra> consultarObraExterna(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PathVariable String codigo,
            @PathVariable String idExterno) {
        IntegracaoMetadadosObra response = adminService.consultarObraExterna(codigo, idExterno, adminLogado.getId());
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 5. FEATURE FLAGS GLOBAIS
    // ==========================================

    @GetMapping("/feature-flags")
    public ResponseEntity<List<FeatureFlagResponse>> listarFeatureFlags(@CurrentUser UsuarioPrincipal adminLogado) {
        List<FeatureFlagResponse> flags = adminService.listarFeatureFlags(adminLogado.getId());
        return ResponseEntity.ok(flags);
    }

    @PutMapping("/feature-flags/{chave}")
    public ResponseEntity<FeatureFlagResponse> alternarFeatureFlag(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PathVariable String chave,
            @Valid @RequestBody AlterarFeatureFlagRequest request) {
        FeatureFlagResponse response = adminService.alternarFeatureFlag(chave, request, adminLogado.getId());
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 6. BROADCAST DE SISTEMA
    // ==========================================

    @PostMapping("/broadcasts")
    public ResponseEntity<BroadcastResponse> criarBroadcast(
            @CurrentUser UsuarioPrincipal adminLogado,
            @Valid @RequestBody CriarBroadcastRequest request) {
        BroadcastResponse response = adminService.criarBroadcast(request, adminLogado.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/broadcasts")
    public ResponseEntity<Page<BroadcastResponse>> listarBroadcasts(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<BroadcastResponse> broadcasts = adminService.listarBroadcasts(pageable, adminLogado.getId());
        return ResponseEntity.ok(broadcasts);
    }

    @DeleteMapping("/broadcasts/{id}")
    public ResponseEntity<Void> desativarBroadcast(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PathVariable Long id) {
        adminService.desativarBroadcast(id, adminLogado.getId());
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // 7. FILAS DE MODERAÇÃO (SUGESTÕES & DENÚNCIAS)
    // ==========================================

    @GetMapping("/sugestoes")
    public ResponseEntity<Page<SugestaoEdicaoResponse>> listarSugestoes(
            @CurrentUser UsuarioPrincipal adminLogado,
            @RequestParam(required = false) dev.shiori.domain.enums.StatusSugestao status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<SugestaoEdicaoResponse> sugestoes = adminService.listarSugestoes(status, pageable, adminLogado.getId());
        return ResponseEntity.ok(sugestoes);
    }

    @GetMapping("/sugestoes/pendentes")
    public ResponseEntity<Page<SugestaoEdicaoResponse>> listarSugestoesPendentes(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<SugestaoEdicaoResponse> sugestoes = adminService.listarSugestoesPendentes(pageable, adminLogado.getId());
        return ResponseEntity.ok(sugestoes);
    }

    @PutMapping("/sugestoes/{sugestaoId}/avaliar")
    public ResponseEntity<SugestaoEdicaoResponse> avaliarSugestao(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PathVariable Long sugestaoId,
            @Valid @RequestBody AvaliarSugestaoRequest request) {
        SugestaoEdicaoResponse response = adminService.avaliarSugestao(sugestaoId, request, adminLogado.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/denuncias/comentarios")
    public ResponseEntity<Page<DenunciaComentarioResponse>> listarDenunciasComentarios(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<DenunciaComentarioResponse> denuncias = adminService.listarDenunciasComentariosPendentes(pageable, adminLogado.getId());
        return ResponseEntity.ok(denuncias);
    }

    @GetMapping("/denuncias/abertas")
    public ResponseEntity<Page<DenunciaComentarioResponse>> listarDenunciasAbertas(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<DenunciaComentarioResponse> denuncias = adminService.listarDenunciasComentariosPendentes(pageable, adminLogado.getId());
        return ResponseEntity.ok(denuncias);
    }

    @PutMapping("/denuncias/comentarios/{denunciaId}/aceitar")
    public ResponseEntity<DenunciaComentarioResponse> aceitarDenunciaComentario(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PathVariable Long denunciaId,
            @RequestBody(required = false) ParecerModeracaoRequest request) {
        String parecer = request != null ? request.parecer() : null;
        DenunciaComentarioResponse response = adminService.resolverDenunciaComentario(denunciaId, true, parecer, adminLogado.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/denuncias/comentarios/{denunciaId}/rejeitar")
    public ResponseEntity<DenunciaComentarioResponse> rejeitarDenunciaComentario(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PathVariable Long denunciaId,
            @RequestBody(required = false) ParecerModeracaoRequest request) {
        String parecer = request != null ? request.parecer() : null;
        DenunciaComentarioResponse response = adminService.resolverDenunciaComentario(denunciaId, false, parecer, adminLogado.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/denuncias/obras")
    public ResponseEntity<Page<DenunciaObraResponse>> listarDenunciasObras(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<DenunciaObraResponse> denuncias = adminService.listarDenunciasObrasPendentes(pageable, adminLogado.getId());
        return ResponseEntity.ok(denuncias);
    }

    @PutMapping("/denuncias/obras/{denunciaId}/aceitar")
    public ResponseEntity<DenunciaObraResponse> aceitarDenunciaObra(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PathVariable Long denunciaId,
            @RequestBody(required = false) ParecerModeracaoRequest request) {
        String parecer = request != null ? request.parecer() : null;
        DenunciaObraResponse response = adminService.resolverDenunciaObra(denunciaId, true, parecer, adminLogado.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/denuncias/obras/{denunciaId}/rejeitar")
    public ResponseEntity<DenunciaObraResponse> rejeitarDenunciaObra(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PathVariable Long denunciaId,
            @RequestBody(required = false) ParecerModeracaoRequest request) {
        String parecer = request != null ? request.parecer() : null;
        DenunciaObraResponse response = adminService.resolverDenunciaObra(denunciaId, false, parecer, adminLogado.getId());
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 8. AUDITORIA NO BANCO
    // ==========================================

    @GetMapping("/auditoria")
    public ResponseEntity<Page<RegistroAuditoriaResponse>> listarAuditoria(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<RegistroAuditoriaResponse> auditoria = adminService.listarAuditoria(pageable, adminLogado.getId());
        return ResponseEntity.ok(auditoria);
    }

    // ==========================================
    // 9. GESTÃO DE INSTÂNCIAS PAREADAS (HANDSHAKE)
    // ==========================================

    @GetMapping("/pareamentos")
    public ResponseEntity<Page<dev.shiori.dto.hub.PareamentoResponse>> listarPareamentos(
            @CurrentUser UsuarioPrincipal adminLogado,
            @RequestParam(required = false) dev.shiori.domain.enums.StatusPareamento status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<dev.shiori.dto.hub.PareamentoResponse> response = adminService.listarPareamentos(status, pageable, adminLogado.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/pareamentos/{id}/decisao")
    public ResponseEntity<dev.shiori.dto.hub.PareamentoResponse> decidirPareamento(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PathVariable Long id,
            @Valid @RequestBody dev.shiori.dto.hub.DecisaoPareamentoRequest request) {
        dev.shiori.dto.hub.PareamentoResponse response = adminService.decidirPareamento(id, request, adminLogado.getId());
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 10. FEDERAÇÃO DE HUBS (MESH)
    // ==========================================

    @GetMapping("/federacao/nos")
    public ResponseEntity<java.util.List<dev.shiori.dto.federacao.NoFederadoResponse>> listarNosFederados(
            @CurrentUser UsuarioPrincipal adminLogado) {
        java.util.List<dev.shiori.dto.federacao.NoFederadoResponse> response = adminService.listarNosFederados(adminLogado.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/federacao/nos")
    public ResponseEntity<dev.shiori.dto.federacao.NoFederadoResponse> cadastrarNoFederado(
            @CurrentUser UsuarioPrincipal adminLogado,
            @Valid @RequestBody dev.shiori.dto.federacao.NoFederadoRequest request) {
        dev.shiori.dto.federacao.NoFederadoResponse response = adminService.cadastrarNoFederado(request, adminLogado.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/federacao/nos/{id}/status")
    public ResponseEntity<dev.shiori.dto.federacao.NoFederadoResponse> alternarStatusNoFederado(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PathVariable Long id,
            @RequestParam boolean ativo) {
        dev.shiori.dto.federacao.NoFederadoResponse response = adminService.alternarStatusNoFederado(id, ativo, adminLogado.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/federacao/nos/{id}")
    public ResponseEntity<Void> removerNoFederado(
            @CurrentUser UsuarioPrincipal adminLogado,
            @PathVariable Long id) {
        adminService.removerNoFederado(id, adminLogado.getId());
        return ResponseEntity.noContent().build();
    }
}
