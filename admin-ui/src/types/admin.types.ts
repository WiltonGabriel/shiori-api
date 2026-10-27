export interface AdminDashboardResponse {
  totalUsuarios: number;
  totalUsuariosAtivos: number;
  totalObras: number;
  totalItensEstante: number;
  totalSugestoesPendentes: number;
  totalDenunciasComentariosPendentes: number;
  totalDenunciasObrasPendentes: number;
}

export interface ProvedorIntegracaoResponse {
  id: number;
  codigo: string;
  nome: string;
  baseUrl: string;
  apiKey?: string | null;
  timeoutMs: number;
  ativo: boolean;
  criadoEm?: string;
  atualizadoEm?: string;
}

export interface FeatureFlagResponse {
  id: number;
  chave: string;
  nome: string;
  descricao: string;
  ativo: boolean;
  atualizadoEm?: string;
}

export interface BroadcastResponse {
  id: number;
  nivel: 'INFO' | 'ALERTA' | 'MANUTENCAO';
  titulo: string;
  mensagem: string;
  ativo: boolean;
  criadoEm: string;
  expiraEm?: string | null;
}

export interface AdminUsuarioResponse {
  id: number;
  apelido: string;
  email: string;
  role: 'ROLE_LEITOR' | 'ROLE_CURADOR' | 'ROLE_MODERADOR' | 'ROLE_ADMIN';
  ativo: boolean;
  pontosConfianca: number;
  totalStrikes: number;
  criadoEm: string;
  onboardingConcluido: boolean;
}

export interface RegistroAuditoriaResponse {
  id: number;
  adminId: number;
  adminApelido: string;
  acao: string;
  alvoTipo: string;
  alvoId: string;
  detalhes?: string | null;
  enderecoIp?: string | null;
  criadoEm: string;
}

export interface SugestaoEdicaoResponse {
  id: number;
  obraId: number;
  obraTitulo: string;
  usuarioId: number;
  usuarioApelido: string;
  status: 'PENDENTE' | 'APROVADA' | 'REJEITADA';
  tituloSugerido?: string | null;
  sinopseSugerida?: string | null;
  justificativa?: string | null;
  parecerCuradoria?: string | null;
  criadoEm: string;
}

export interface DenunciaComentarioResponse {
  id: number;
  comentarioId: number;
  conteudoComentario: string;
  autorComentarioApelido: string;
  autorComentarioId: number;
  denuncianteId: number;
  denuncianteApelido: string;
  motivo: string;
  status: string;
  criadoEm: string;
}

export interface DenunciaObraResponse {
  id: number;
  obraId: number;
  obraTitulo: string;
  denuncianteId: number;
  denuncianteApelido: string;
  motivo: string;
  status: string;
  criadoEm: string;
}

export interface GeneroResponse {
  id: number;
  nome: string;
  slug: string;
  descricao?: string | null;
}

export interface NoFederadoResponse {
  id: number;
  nome: string;
  urlBase: string;
  ativo: boolean;
  versaoApi?: string | null;
  ultimoSyncEm?: string | null;
}

export interface PareamentoResponse {
  id: number;
  instanciaUuid: string;
  nomeDispositivo: string;
  status: 'PENDENTE' | 'APROVADO' | 'REJEITADO' | 'REVOGADO';
  criadoEm: string;
}

export interface ObraResponse {
  id: number;
  titulo: string;
  tipo: string;
  status: string;
  autor?: string | null;
  sinopse?: string | null;
  generos?: string[];
  oficial: boolean;
}

export interface AuthResponse {
  token?: string | null;
  id: number;
  email: string;
  apelido: string;
  role: string;
  onboardingConcluido: boolean;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface HealthCheckResponse {
  status: string;
  components?: Record<string, { status: string; details?: Record<string, unknown> }>;
}

export interface AppInfoResponse {
  app?: { name?: string; version?: string };
  build?: { version?: string; time?: string };
  git?: { commit?: { id?: string } };
}

export interface SugestaoTagAdminResponse {
  id: number;
  obraId: number;
  obraTitulo: string;
  tagId?: number | null;
  nomeTag: string;
  slugTag: string;
  status: 'PENDENTE' | 'APROVADA' | 'REJEITADA';
  usuarioSugeridorApelido: string;
  justificativa?: string | null;
  dataSugestao: string;
  adminAvaliadorApelido?: string | null;
  dataAvaliacao?: string | null;
  motivoRejeicao?: string | null;
}

export interface TagObraResponse {
  tagId: number;
  nome: string;
  slug: string;
  score: number;
  meuVoto?: 'POSITIVO' | 'NEGATIVO' | null;
}

export interface ObraBuscaAvancadaFiltro {
  termo?: string;
  tipos?: string[];
  status?: string[];
  ordenacao?: 'NOTA_DESC' | 'NOTA_ASC' | 'CAPITULOS_DESC' | 'CAPITULOS_ASC' | 'TITULO_ASC' | 'TITULO_DESC' | 'RECENTES' | 'ANTIGAS';
  minNota?: number;
  maxNota?: number;
  minCapitulos?: number;
  maxCapitulos?: number;
  pagina?: number;
  itensPorPagina?: number;
}
