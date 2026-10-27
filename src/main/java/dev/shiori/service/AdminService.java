package dev.shiori.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.shiori.dto.ObraResponse;
import dev.shiori.dto.admin.AdminDashboardResponse;
import dev.shiori.dto.admin.AdminUsuarioResponse;
import dev.shiori.dto.admin.AlterarFeatureFlagRequest;
import dev.shiori.dto.admin.AlterarRoleUsuarioRequest;
import dev.shiori.dto.admin.AlterarStatusUsuarioRequest;
import dev.shiori.dto.admin.AplicarStrikeRequest;
import dev.shiori.dto.admin.AvaliarSugestaoRequest;
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
import dev.shiori.dto.admin.ProvedorIntegracaoResponse;
import dev.shiori.dto.admin.RegistroAuditoriaResponse;
import dev.shiori.dto.admin.SalvarProvedorIntegracaoRequest;
import dev.shiori.dto.admin.SugestaoEdicaoResponse;
import dev.shiori.infra.integration.IntegracaoMetadadosObra;
import dev.shiori.service.admin.AdminAuditoriaService;
import dev.shiori.service.admin.AdminConfigService;
import dev.shiori.service.admin.AdminCuradoriaService;
import dev.shiori.service.admin.AdminDashboardService;
import dev.shiori.service.admin.AdminIntegracaoService;
import dev.shiori.service.admin.AdminModeracaoService;

/**
 * Fachada Administrativa (Admin Facade).
 * Delega operações aos serviços especializados de domínio administrativo:
 * - {@link AdminDashboardService}: Métricas e visão executiva.
 * - {@link AdminModeracaoService}: Gestão de usuários, strikes e denúncias.
 * - {@link AdminCuradoriaService}: Mesclagens, overrides, gêneros e tags.
 * - {@link AdminIntegracaoService}: Provedores externos e cotas.
 * - {@link AdminConfigService}: Feature flags e broadcasts.
 * - {@link AdminAuditoriaService}: Auditoria de segurança e validação.
 * 
 * Segue estritamente os princípios SOLID (SRP, ISP) e Clean Code.
 */
@Service
@Transactional
public class AdminService {

    private final AdminDashboardService dashboardService;
    private final AdminModeracaoService moderacaoService;
    private final AdminCuradoriaService curadoriaService;
    private final AdminIntegracaoService integracaoService;
    private final AdminConfigService configService;
    private final AdminAuditoriaService auditoriaService;
    private final PareamentoService pareamentoService;
    private final FederacaoService federacaoService;

    @org.springframework.beans.factory.annotation.Autowired
    public AdminService(AdminDashboardService dashboardService,
                        AdminModeracaoService moderacaoService,
                        AdminCuradoriaService curadoriaService,
                        AdminIntegracaoService integracaoService,
                        AdminConfigService configService,
                        AdminAuditoriaService auditoriaService,
                        PareamentoService pareamentoService,
                        FederacaoService federacaoService) {
        this.dashboardService = dashboardService;
        this.moderacaoService = moderacaoService;
        this.curadoriaService = curadoriaService;
        this.integracaoService = integracaoService;
        this.configService = configService;
        this.auditoriaService = auditoriaService;
        this.pareamentoService = pareamentoService;
        this.federacaoService = federacaoService;
    }

    public AdminService(AdminDashboardService dashboardService,
                        AdminModeracaoService moderacaoService,
                        AdminCuradoriaService curadoriaService,
                        AdminIntegracaoService integracaoService,
                        AdminConfigService configService,
                        AdminAuditoriaService auditoriaService) {
        this(dashboardService, moderacaoService, curadoriaService, integracaoService, configService, auditoriaService, null, null);
    }

    // --- Dashboard & Métricas ---
    @Transactional(readOnly = true)
    public AdminDashboardResponse obterDashboard(Long adminId) {
        return dashboardService.obterDashboard(adminId);
    }

    // --- Gestão de Usuários & Moderação ---
    @Transactional(readOnly = true)
    public Page<AdminUsuarioResponse> listarUsuarios(Pageable pageable, Long adminId) {
        return moderacaoService.listarUsuarios(pageable, adminId);
    }

    public AdminUsuarioResponse alterarStatusUsuario(Long usuarioId, AlterarStatusUsuarioRequest request, Long adminId) {
        return moderacaoService.alterarStatusUsuario(usuarioId, request, adminId);
    }

    public AdminUsuarioResponse alterarRoleUsuario(Long usuarioId, AlterarRoleUsuarioRequest request, Long adminId) {
        return moderacaoService.alterarRoleUsuario(usuarioId, request, adminId);
    }

    public void banirUsuario(Long usuarioId, String motivo, Long adminId) {
        moderacaoService.banirUsuario(usuarioId, motivo, adminId);
    }

    public AdminUsuarioResponse aplicarStrikeUsuario(Long usuarioId, AplicarStrikeRequest request, Long adminId) {
        return moderacaoService.aplicarStrikeUsuario(usuarioId, request, adminId);
    }

    public AdminUsuarioResponse removerStrikeUsuario(Long usuarioId, String motivo, Long adminId) {
        return moderacaoService.removerStrikeUsuario(usuarioId, motivo, adminId);
    }

    // --- Curadoria de Obras e Metadados ---
    public MesclarObrasResponse mesclarObras(MesclarObrasRequest request, Long adminId) {
        return curadoriaService.mesclarObras(request, adminId);
    }

    public ObraResponse oficializarObra(Long obraId, Long adminId) {
        return curadoriaService.oficializarObra(obraId, adminId);
    }

    public ObraResponse aplicarOverrideObra(Long obraId, OverrideObraRequest request, Long adminId) {
        return curadoriaService.aplicarOverrideObra(obraId, request, adminId);
    }

    public GeneroResponse criarGenero(CriarGeneroRequest request, Long adminId) {
        return curadoriaService.criarGenero(request, adminId);
    }

    @Transactional(readOnly = true)
    public List<GeneroResponse> listarGeneros(Long adminId) {
        return curadoriaService.listarGeneros(adminId);
    }

    public MesclarTagsResponse mesclarTags(MesclarTagsRequest request, Long adminId) {
        return curadoriaService.mesclarTags(request, adminId);
    }

    // --- Provedores de Integração ---
    @Transactional(readOnly = true)
    public List<ProvedorIntegracaoResponse> listarProvedoresIntegracao(Long adminId) {
        return integracaoService.listarProvedoresIntegracao(adminId);
    }

    public ProvedorIntegracaoResponse salvarProvedorIntegracao(SalvarProvedorIntegracaoRequest request, Long adminId) {
        return integracaoService.salvarProvedorIntegracao(request, adminId);
    }

    public ProvedorIntegracaoResponse alternarStatusProvedor(Long id, boolean ativo, Long adminId) {
        return integracaoService.alternarStatusProvedor(id, ativo, adminId);
    }

    public IntegracaoMetadadosObra consultarObraExterna(String codigoProvedor, String idExterno, Long adminId) {
        return integracaoService.consultarObraExterna(codigoProvedor, idExterno, adminId);
    }

    // --- Feature Flags Globais ---
    @Transactional(readOnly = true)
    public List<FeatureFlagResponse> listarFeatureFlags(Long adminId) {
        return configService.listarFeatureFlags(adminId);
    }

    public FeatureFlagResponse alternarFeatureFlag(String chave, AlterarFeatureFlagRequest request, Long adminId) {
        return configService.alternarFeatureFlag(chave, request, adminId);
    }

    @Transactional(readOnly = true)
    public boolean isFeatureFlagAtiva(String chave) {
        return configService.isFeatureFlagAtiva(chave);
    }

    // --- Broadcasts de Sistema ---
    public BroadcastResponse criarBroadcast(CriarBroadcastRequest request, Long adminId) {
        return configService.criarBroadcast(request, adminId);
    }

    @Transactional(readOnly = true)
    public Page<BroadcastResponse> listarBroadcasts(Pageable pageable, Long adminId) {
        return configService.listarBroadcasts(pageable, adminId);
    }

    @Transactional(readOnly = true)
    public List<BroadcastResponse> listarBroadcastsAtivos() {
        return configService.listarBroadcastsAtivos();
    }

    public void desativarBroadcast(Long id, Long adminId) {
        configService.desativarBroadcast(id, adminId);
    }

    // --- Moderação de Sugestões e Denúncias ---
    @Transactional(readOnly = true)
    public Page<SugestaoEdicaoResponse> listarSugestoesPendentes(Pageable pageable, Long adminId) {
        return curadoriaService.listarSugestoesPendentes(pageable, adminId);
    }

    public Page<SugestaoEdicaoResponse> listarSugestoes(dev.shiori.domain.enums.StatusSugestao status, Pageable pageable, Long adminId) {
        return curadoriaService.listarSugestoes(status, pageable, adminId);
    }

    public SugestaoEdicaoResponse avaliarSugestao(Long sugestaoId, AvaliarSugestaoRequest request, Long adminId) {
        return curadoriaService.avaliarSugestao(sugestaoId, request, adminId);
    }

    @Transactional(readOnly = true)
    public Page<DenunciaComentarioResponse> listarDenunciasComentariosPendentes(Pageable pageable, Long adminId) {
        return moderacaoService.listarDenunciasComentariosPendentes(pageable, adminId);
    }

    public DenunciaComentarioResponse resolverDenunciaComentario(Long denunciaId, boolean aceitar, String parecer, Long adminId) {
        return moderacaoService.resolverDenunciaComentario(denunciaId, aceitar, parecer, adminId);
    }

    @Transactional(readOnly = true)
    public Page<DenunciaObraResponse> listarDenunciasObrasPendentes(Pageable pageable, Long adminId) {
        return moderacaoService.listarDenunciasObrasPendentes(pageable, adminId);
    }

    public DenunciaObraResponse resolverDenunciaObra(Long denunciaId, boolean aceitar, String parecer, Long adminId) {
        return moderacaoService.resolverDenunciaObra(denunciaId, aceitar, parecer, adminId);
    }

    // --- Auditoria ---
    @Transactional(readOnly = true)
    public Page<RegistroAuditoriaResponse> listarAuditoria(Pageable pageable, Long adminId) {
        return auditoriaService.listarAuditoria(pageable, adminId);
    }

    // --- Pareamento de Instâncias ---
    @Transactional(readOnly = true)
    public Page<dev.shiori.dto.hub.PareamentoResponse> listarPareamentos(dev.shiori.domain.enums.StatusPareamento status, Pageable pageable, Long adminId) {
        auditoriaService.obterEValidarAdmin(adminId);
        return pareamentoService.listarInstancias(status, pageable);
    }

    public dev.shiori.dto.hub.PareamentoResponse decidirPareamento(Long id, dev.shiori.dto.hub.DecisaoPareamentoRequest request, Long adminId) {
        auditoriaService.obterEValidarAdmin(adminId);
        return pareamentoService.decidirPareamento(id, request);
    }

    // --- Federação de Hubs (Mesh) ---
    @Transactional(readOnly = true)
    public java.util.List<dev.shiori.dto.federacao.NoFederadoResponse> listarNosFederados(Long adminId) {
        auditoriaService.obterEValidarAdmin(adminId);
        return federacaoService.listarNos();
    }

    public dev.shiori.dto.federacao.NoFederadoResponse cadastrarNoFederado(dev.shiori.dto.federacao.NoFederadoRequest request, Long adminId) {
        auditoriaService.obterEValidarAdmin(adminId);
        return federacaoService.cadastrarNo(request);
    }

    public dev.shiori.dto.federacao.NoFederadoResponse alternarStatusNoFederado(Long id, boolean ativo, Long adminId) {
        auditoriaService.obterEValidarAdmin(adminId);
        return federacaoService.alternarStatusNo(id, ativo);
    }

    public void removerNoFederado(Long id, Long adminId) {
        auditoriaService.obterEValidarAdmin(adminId);
        federacaoService.removerNo(id);
    }
}
