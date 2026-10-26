import { request } from './api.service';
import type {
  AdminDashboardResponse,
  ProvedorIntegracaoResponse,
  FeatureFlagResponse,
  BroadcastResponse,
  AdminUsuarioResponse,
  RegistroAuditoriaResponse,
  SugestaoEdicaoResponse,
  DenunciaComentarioResponse,
  DenunciaObraResponse,
  GeneroResponse,
  NoFederadoResponse,
  PareamentoResponse,
  ObraResponse,
  PageResponse,
  HealthCheckResponse,
  AppInfoResponse,
  AuthResponse,
  SugestaoTagAdminResponse,
  TagObraResponse,
  ObraBuscaAvancadaFiltro,
} from '../types/admin.types';

export const AdminService = {
  // 1. Autenticação e Perfil
  login(email: string, senha: string, remember: boolean = false): Promise<AuthResponse> {
    return request<AuthResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, senha, rememberMe: remember }),
    });
  },

  getMe(): Promise<AuthResponse> {
    return request<AuthResponse>('/api/auth/me');
  },

  // 2. Dashboard e Saúde
  getDashboard(): Promise<AdminDashboardResponse> {
    return request<AdminDashboardResponse>('/api/admin/dashboard');
  },

  getHealth(): Promise<HealthCheckResponse> {
    return request<HealthCheckResponse>('/actuator/health');
  },

  getInfo(): Promise<AppInfoResponse> {
    return request<AppInfoResponse>('/actuator/info');
  },

  getAuditoria(page: number = 0, size: number = 20): Promise<PageResponse<RegistroAuditoriaResponse>> {
    return request<PageResponse<RegistroAuditoriaResponse>>(`/api/admin/auditoria?page=${page}&size=${size}&sort=criadoEm,desc`);
  },

  // 3. Provedores de Integração Bibliográfica
  listarIntegracoes(): Promise<ProvedorIntegracaoResponse[]> {
    return request<ProvedorIntegracaoResponse[]>('/api/admin/integracoes');
  },

  salvarIntegracao(data: {
    codigo: string;
    nome: string;
    baseUrl: string;
    apiKey?: string | null;
    timeoutMs: number;
    ativo: boolean;
  }): Promise<ProvedorIntegracaoResponse> {
    return request<ProvedorIntegracaoResponse>('/api/admin/integracoes', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  alternarStatusIntegracao(id: number, ativo: boolean): Promise<ProvedorIntegracaoResponse> {
    return request<ProvedorIntegracaoResponse>(`/api/admin/integracoes/${id}/status?ativo=${ativo}`, {
      method: 'PUT',
    });
  },

  testarConsultaExterna(codigo: string, idExterno: string): Promise<any> {
    return request<any>(`/api/admin/integracoes/${encodeURIComponent(codigo)}/obras/${encodeURIComponent(idExterno)}`);
  },

  // 4. Feature Flags e Configurações
  listarFeatureFlags(): Promise<FeatureFlagResponse[]> {
    return request<FeatureFlagResponse[]>('/api/admin/feature-flags');
  },

  alternarFeatureFlag(chave: string, ativo: boolean, justificativa: string): Promise<FeatureFlagResponse> {
    return request<FeatureFlagResponse>(`/api/admin/feature-flags/${encodeURIComponent(chave)}`, {
      method: 'PUT',
      body: JSON.stringify({ ativo, justificativa }),
    });
  },

  // 5. Broadcasts de Sistema
  listarBroadcasts(page: number = 0, size: number = 20): Promise<PageResponse<BroadcastResponse>> {
    return request<PageResponse<BroadcastResponse>>(`/api/admin/broadcasts?page=${page}&size=${size}`);
  },

  criarBroadcast(data: {
    nivel: 'INFO' | 'ALERTA' | 'MANUTENCAO';
    titulo: string;
    mensagem: string;
    expiraEm?: string | null;
  }): Promise<BroadcastResponse> {
    return request<BroadcastResponse>('/api/admin/broadcasts', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  desativarBroadcast(id: number): Promise<void> {
    return request<void>(`/api/admin/broadcasts/${id}`, {
      method: 'DELETE',
    });
  },

  // 6. Curadoria do Catálogo
  buscarObras(query: string = '', page: number = 0, size: number = 20): Promise<PageResponse<ObraResponse>> {
    const q = query ? `&q=${encodeURIComponent(query)}` : '';
    return request<PageResponse<ObraResponse>>(`/api/obras?page=${page}&size=${size}${q}`);
  },

  buscarObrasAvancada(filtro: ObraBuscaAvancadaFiltro): Promise<PageResponse<ObraResponse>> {
    const params = new URLSearchParams();
    if (filtro.termo) params.append('termo', filtro.termo);
    if (filtro.tipos?.length) filtro.tipos.forEach(t => params.append('tipos', t));
    if (filtro.status?.length) filtro.status.forEach(s => params.append('status', s));
    if (filtro.ordenacao) params.append('ordenacao', filtro.ordenacao);
    if (filtro.minCapitulos !== undefined) params.append('minCapitulos', String(filtro.minCapitulos));
    if (filtro.maxCapitulos !== undefined) params.append('maxCapitulos', String(filtro.maxCapitulos));
    if (filtro.minNota !== undefined) params.append('minNota', String(filtro.minNota));
    if (filtro.maxNota !== undefined) params.append('maxNota', String(filtro.maxNota));
    if (filtro.pagina !== undefined) params.append('pagina', String(filtro.pagina));
    if (filtro.itensPorPagina !== undefined) params.append('itensPorPagina', String(filtro.itensPorPagina));

    const qs = params.toString();
    return request<PageResponse<ObraResponse>>(`/api/obras/busca-avancada${qs ? `?${qs}` : ''}`);
  },

  listarTagsDaObra(obraId: number): Promise<TagObraResponse[]> {
    return request<TagObraResponse[]>(`/api/tags/obras/${obraId}`);
  },

  mesclarObras(idObraPrincipal: number, idObraDuplicada: number): Promise<any> {
    return request<any>('/api/admin/obras/mesclar', {
      method: 'POST',
      body: JSON.stringify({ idObraPrincipal, idObraDuplicada }),
    });
  },

  oficializarObra(id: number): Promise<ObraResponse> {
    return request<ObraResponse>(`/api/admin/obras/${id}/oficializar`, {
      method: 'PUT',
    });
  },

  overrideObra(id: number, data: {
    titulo?: string;
    sinopse?: string;
    autor?: string;
    statusObra?: string;
    motivoOverride: string;
  }): Promise<ObraResponse> {
    return request<ObraResponse>(`/api/admin/obras/${id}/override`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  listarGeneros(): Promise<GeneroResponse[]> {
    return request<GeneroResponse[]>('/api/admin/generos');
  },

  criarGenero(nome: string, descricao?: string): Promise<GeneroResponse> {
    return request<GeneroResponse>('/api/admin/generos', {
      method: 'POST',
      body: JSON.stringify({ nome, descricao }),
    });
  },

  mesclarTags(tagOrigemId: number, tagDestinoId: number, motivo: string): Promise<any> {
    return request<any>('/api/admin/tags/mesclar', {
      method: 'POST',
      body: JSON.stringify({ tagOrigemId, tagDestinoId, motivo }),
    });
  },

  // 7. Moderação Comunitária
  listarSugestoesPendentes(page: number = 0, size: number = 20): Promise<PageResponse<SugestaoEdicaoResponse>> {
    return request<PageResponse<SugestaoEdicaoResponse>>(`/api/admin/sugestoes/pendentes?page=${page}&size=${size}`);
  },

  avaliarSugestao(sugestaoId: number, aprovada: boolean, parecer?: string): Promise<SugestaoEdicaoResponse> {
    return request<SugestaoEdicaoResponse>(`/api/admin/sugestoes/${sugestaoId}/avaliar`, {
      method: 'PUT',
      body: JSON.stringify({ aprovada, parecer: parecer || '' }),
    });
  },

  listarDenunciasComentarios(page: number = 0, size: number = 20): Promise<PageResponse<DenunciaComentarioResponse>> {
    return request<PageResponse<DenunciaComentarioResponse>>(`/api/admin/denuncias/comentarios?page=${page}&size=${size}`);
  },

  aceitarDenunciaComentario(id: number, parecer?: string): Promise<DenunciaComentarioResponse> {
    return request<DenunciaComentarioResponse>(`/api/admin/denuncias/comentarios/${id}/aceitar`, {
      method: 'PUT',
      body: JSON.stringify({ parecer }),
    });
  },

  rejeitarDenunciaComentario(id: number, parecer?: string): Promise<DenunciaComentarioResponse> {
    return request<DenunciaComentarioResponse>(`/api/admin/denuncias/comentarios/${id}/rejeitar`, {
      method: 'PUT',
      body: JSON.stringify({ parecer }),
    });
  },

  listarDenunciasObras(page: number = 0, size: number = 20): Promise<PageResponse<DenunciaObraResponse>> {
    return request<PageResponse<DenunciaObraResponse>>(`/api/admin/denuncias/obras?page=${page}&size=${size}`);
  },

  aceitarDenunciaObra(id: number, parecer?: string): Promise<DenunciaObraResponse> {
    return request<DenunciaObraResponse>(`/api/admin/denuncias/obras/${id}/aceitar`, {
      method: 'PUT',
      body: JSON.stringify({ parecer }),
    });
  },

  rejeitarDenunciaObra(id: number, parecer?: string): Promise<DenunciaObraResponse> {
    return request<DenunciaObraResponse>(`/api/admin/denuncias/obras/${id}/rejeitar`, {
      method: 'PUT',
      body: JSON.stringify({ parecer }),
    });
  },

  // Governança de Tags Comunitárias (Incremento 2.2)
  listarSugestoesTags(status?: string, page: number = 0, size: number = 20): Promise<PageResponse<SugestaoTagAdminResponse>> {
    const statusParam = status && status !== 'TODAS' ? `&status=${status}` : '';
    return request<PageResponse<SugestaoTagAdminResponse>>(`/api/admin/tags/sugestoes?page=${page}&size=${size}${statusParam}`);
  },

  aprovarSugestaoTag(id: number): Promise<SugestaoTagAdminResponse> {
    return request<SugestaoTagAdminResponse>(`/api/admin/tags/sugestoes/${id}/aprovar`, {
      method: 'POST',
    });
  },

  rejeitarSugestaoTag(id: number, motivo: string): Promise<SugestaoTagAdminResponse> {
    return request<SugestaoTagAdminResponse>(`/api/admin/tags/sugestoes/${id}/rejeitar`, {
      method: 'POST',
      body: JSON.stringify({ motivo }),
    });
  },

  // 8. Gestão de Usuários
  listarUsuarios(page: number = 0, size: number = 20): Promise<PageResponse<AdminUsuarioResponse>> {
    return request<PageResponse<AdminUsuarioResponse>>(`/api/admin/usuarios?page=${page}&size=${size}`);
  },

  alterarStatusUsuario(usuarioId: number, ativo: boolean, motivo: string): Promise<AdminUsuarioResponse> {
    return request<AdminUsuarioResponse>(`/api/admin/usuarios/${usuarioId}/status`, {
      method: 'PUT',
      body: JSON.stringify({ ativo, motivo }),
    });
  },

  alterarRoleUsuario(usuarioId: number, role: string, motivo: string): Promise<AdminUsuarioResponse> {
    return request<AdminUsuarioResponse>(`/api/admin/usuarios/${usuarioId}/role`, {
      method: 'PUT',
      body: JSON.stringify({ role, motivo }),
    });
  },

  aplicarStrike(usuarioId: number, motivo: string): Promise<AdminUsuarioResponse> {
    return request<AdminUsuarioResponse>(`/api/admin/usuarios/${usuarioId}/strikes`, {
      method: 'POST',
      body: JSON.stringify({ motivo }),
    });
  },

  removerStrike(usuarioId: number, motivo: string): Promise<AdminUsuarioResponse> {
    return request<AdminUsuarioResponse>(`/api/admin/usuarios/${usuarioId}/strikes?motivo=${encodeURIComponent(motivo)}`, {
      method: 'DELETE',
    });
  },

  banirUsuario(usuarioId: number, motivo: string): Promise<void> {
    return request<void>(`/api/admin/usuarios/${usuarioId}/banir`, {
      method: 'PUT',
      body: JSON.stringify({ motivo }),
    });
  },

  // 9. Federação e Pareamento
  listarNosFederados(): Promise<NoFederadoResponse[]> {
    return request<NoFederadoResponse[]>('/api/admin/federacao/nos');
  },

  cadastrarNoFederado(data: { nome: string; urlBase: string; chavePublicaMesh?: string }): Promise<NoFederadoResponse> {
    return request<NoFederadoResponse>('/api/admin/federacao/nos', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  alternarStatusNoFederado(id: number, ativo: boolean): Promise<NoFederadoResponse> {
    return request<NoFederadoResponse>(`/api/admin/federacao/nos/${id}/status?ativo=${ativo}`, {
      method: 'PUT',
    });
  },

  removerNoFederado(id: number): Promise<void> {
    return request<void>(`/api/admin/federacao/nos/${id}`, {
      method: 'DELETE',
    });
  },

  listarPareamentos(status?: string, page: number = 0, size: number = 20): Promise<PageResponse<PareamentoResponse>> {
    const s = status ? `&status=${status}` : '';
    return request<PageResponse<PareamentoResponse>>(`/api/admin/pareamentos?page=${page}&size=${size}${s}`);
  },

  decidirPareamento(id: number, aprovado: boolean, motivo?: string): Promise<PareamentoResponse> {
    return request<PareamentoResponse>(`/api/admin/pareamentos/${id}/decisao`, {
      method: 'POST',
      body: JSON.stringify({ aprovado, motivo }),
    });
  },
};
