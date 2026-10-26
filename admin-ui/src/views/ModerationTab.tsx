import React, { useEffect, useState } from 'react';
import {
  FileQuestion,
  MessageSquare,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  RefreshCw,
  Tag as TagIcon,
  Filter,
} from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Modal } from '../components/ui/Modal';
import { AdminService } from '../services/admin.service';
import { toast } from '../components/ui/Toast';
import type {
  SugestaoEdicaoResponse,
  DenunciaComentarioResponse,
  DenunciaObraResponse,
  SugestaoTagAdminResponse,
} from '../types/admin.types';

export const ModerationTab: React.FC = () => {
  const [subTab, setSubTab] = useState<'TAGS' | 'SUGESTOES' | 'COMENTARIOS' | 'OBRAS'>('TAGS');

  // Filas de Dados
  const [sugestoesTags, setSugestoesTags] = useState<SugestaoTagAdminResponse[]>([]);
  const [totalPaginasTags, setTotalPaginasTags] = useState(1);
  const [paginaTags, setPaginaTags] = useState(0);
  const [filtroStatusTag, setFiltroStatusTag] = useState<'PENDENTE' | 'APROVADA' | 'REJEITADA' | 'TODAS'>('PENDENTE');
  const [isAprovandoTagId, setIsAprovandoTagId] = useState<number | null>(null);

  const [sugestoes, setSugestoes] = useState<SugestaoEdicaoResponse[]>([]);
  const [denunciasComentarios, setDenunciasComentarios] = useState<DenunciaComentarioResponse[]>([]);
  const [denunciasObras, setDenunciasObras] = useState<DenunciaObraResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Modal Rejeitar Sugestão de Tag (Incremento 2.2)
  const [selectedRejeitarTag, setSelectedRejeitarTag] = useState<SugestaoTagAdminResponse | null>(null);
  const [motivoRejeicaoTag, setMotivoRejeicaoTag] = useState('');
  const [isSubmittingRejeicaoTag, setIsSubmittingRejeicaoTag] = useState(false);

  // Modal Avaliar Sugestão de Edição
  const [selectedSugestao, setSelectedSugestao] = useState<{ id: number; aprovada: boolean; obraTitulo: string } | null>(null);
  const [parecerSugestao, setParecerSugestao] = useState('');
  const [isSubmittingSugestao, setIsSubmittingSugestao] = useState(false);

  // Modal Parecer Denúncia Comentário
  const [selectedDenunciaComentario, setSelectedDenunciaComentario] = useState<{ id: number; aceitar: boolean } | null>(null);
  const [parecerComentario, setParecerComentario] = useState('');
  const [isSubmittingComentario, setIsSubmittingComentario] = useState(false);

  // Modal Parecer Denúncia Obra
  const [selectedDenunciaObra, setSelectedDenunciaObra] = useState<{ id: number; aceitar: boolean } | null>(null);
  const [parecerObra, setParecerObra] = useState('');
  const [isSubmittingObra, setIsSubmittingObra] = useState(false);

  const carregarTags = async (status: string = filtroStatusTag, pag: number = paginaTags) => {
    try {
      const res = await AdminService.listarSugestoesTags(status, pag, 15);
      setSugestoesTags(res.content || []);
      setTotalPaginasTags(res.totalPages || 1);
    } catch (err: any) {
      toast.error(err.message || 'Erro ao carregar sugestões de tags.');
    }
  };

  const carregarDadosGerais = async () => {
    try {
      setIsLoading(true);
      const [sugRes, denComRes, denObraRes] = await Promise.allSettled([
        AdminService.listarSugestoesPendentes(),
        AdminService.listarDenunciasComentarios(),
        AdminService.listarDenunciasObras(),
      ]);

      if (sugRes.status === 'fulfilled') setSugestoes(sugRes.value.content || []);
      if (denComRes.status === 'fulfilled') setDenunciasComentarios(denComRes.value.content || []);
      if (denObraRes.status === 'fulfilled') setDenunciasObras(denObraRes.value.content || []);

      await carregarTags(filtroStatusTag, paginaTags);
    } catch {
      toast.error('Erro ao carregar filas de moderação.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    carregarDadosGerais();
  }, []);

  useEffect(() => {
    carregarTags(filtroStatusTag, paginaTags);
  }, [filtroStatusTag, paginaTags]);

  // Ações de Governança de Tags (Incremento 2.2)
  const handleAprovarTag = async (tag: SugestaoTagAdminResponse) => {
    try {
      setIsAprovandoTagId(tag.id);
      await AdminService.aprovarSugestaoTag(tag.id);
      toast.success(`Tag '#${tag.nomeTag}' aprovada e associada à obra '${tag.obraTitulo}'!`);
      await carregarTags(filtroStatusTag, paginaTags);
    } catch (err: any) {
      toast.error(err.message || 'Erro ao aprovar sugestão de tag.');
    } finally {
      setIsAprovandoTagId(null);
    }
  };

  const handleConfirmarRejeicaoTag = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedRejeitarTag) return;
    if (!motivoRejeicaoTag.trim()) {
      toast.error('O motivo da rejeição é obrigatório.');
      return;
    }
    if (motivoRejeicaoTag.trim().length > 255) {
      toast.error('O motivo não pode exceder 255 caracteres.');
      return;
    }

    try {
      setIsSubmittingRejeicaoTag(true);
      await AdminService.rejeitarSugestaoTag(selectedRejeitarTag.id, motivoRejeicaoTag.trim());
      toast.success(`Sugestão da tag '#${selectedRejeitarTag.nomeTag}' rejeitada com justificativa registrada.`);
      setSelectedRejeitarTag(null);
      setMotivoRejeicaoTag('');
      await carregarTags(filtroStatusTag, paginaTags);
    } catch (err: any) {
      toast.error(err.message || 'Erro ao rejeitar sugestão de tag.');
    } finally {
      setIsSubmittingRejeicaoTag(false);
    }
  };

  // Ações de Edições e Denúncias
  const handleConfirmarSugestao = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedSugestao) return;

    try {
      setIsSubmittingSugestao(true);
      await AdminService.avaliarSugestao(
        selectedSugestao.id,
        selectedSugestao.aprovada,
        parecerSugestao.trim() || undefined
      );
      setSugestoes((prev) => prev.filter((s) => s.id !== selectedSugestao.id));
      toast.success(`Sugestão ${selectedSugestao.aprovada ? 'aprovada' : 'rejeitada'} com sucesso!`);
      setSelectedSugestao(null);
    } catch (err: any) {
      toast.error(err.message || 'Erro ao avaliar sugestão.');
    } finally {
      setIsSubmittingSugestao(false);
    }
  };

  const handleConfirmarDenunciaComentario = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedDenunciaComentario) return;

    try {
      setIsSubmittingComentario(true);
      if (selectedDenunciaComentario.aceitar) {
        await AdminService.aceitarDenunciaComentario(selectedDenunciaComentario.id, parecerComentario.trim() || undefined);
        toast.success('Denúncia aceita! Comentário ofensivo moderado.');
      } else {
        await AdminService.rejeitarDenunciaComentario(selectedDenunciaComentario.id, parecerComentario.trim() || undefined);
        toast.success('Denúncia rejeitada.');
      }
      setDenunciasComentarios((prev) => prev.filter((d) => d.id !== selectedDenunciaComentario.id));
      setSelectedDenunciaComentario(null);
    } catch (err: any) {
      toast.error(err.message || 'Erro ao processar denúncia de comentário.');
    } finally {
      setIsSubmittingComentario(false);
    }
  };

  const handleConfirmarDenunciaObra = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedDenunciaObra) return;

    try {
      setIsSubmittingObra(true);
      if (selectedDenunciaObra.aceitar) {
        await AdminService.aceitarDenunciaObra(selectedDenunciaObra.id, parecerObra.trim() || undefined);
        toast.success('Denúncia aceita com parecer registrado.');
      } else {
        await AdminService.rejeitarDenunciaObra(selectedDenunciaObra.id, parecerObra.trim() || undefined);
        toast.success('Denúncia rejeitada.');
      }
      setDenunciasObras((prev) => prev.filter((d) => d.id !== selectedDenunciaObra.id));
      setSelectedDenunciaObra(null);
    } catch (err: any) {
      toast.error(err.message || 'Erro ao processar denúncia de obra.');
    } finally {
      setIsSubmittingObra(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Top Banner */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '22px', fontWeight: 700 }}>Filas de Moderação & Curadoria Comunitária</h2>
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginTop: '2px' }}>
            Governança de tags propostas pela comunidade, edições de catálogo e moderação de denúncias de abuso.
          </p>
        </div>
        <Button variant="secondary" size="sm" onClick={carregarDadosGerais} isLoading={isLoading}>
          <RefreshCw size={14} />
          <span>Atualizar Filas</span>
        </Button>
      </div>

      {/* Subtabs de Navegação */}
      <div style={{ display: 'flex', gap: '8px', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '12px', flexWrap: 'wrap' }}>
        <Button
          variant={subTab === 'TAGS' ? 'primary' : 'ghost'}
          size="sm"
          onClick={() => setSubTab('TAGS')}
        >
          <TagIcon size={14} />
          <span>Tags Comunitárias ({sugestoesTags.length})</span>
        </Button>
        <Button
          variant={subTab === 'SUGESTOES' ? 'primary' : 'ghost'}
          size="sm"
          onClick={() => setSubTab('SUGESTOES')}
        >
          <FileQuestion size={14} />
          <span>Sugestões de Edição ({sugestoes.length})</span>
        </Button>
        <Button
          variant={subTab === 'COMENTARIOS' ? 'primary' : 'ghost'}
          size="sm"
          onClick={() => setSubTab('COMENTARIOS')}
        >
          <MessageSquare size={14} />
          <span>Denúncias de Comentários ({denunciasComentarios.length})</span>
        </Button>
        <Button
          variant={subTab === 'OBRAS' ? 'primary' : 'ghost'}
          size="sm"
          onClick={() => setSubTab('OBRAS')}
        >
          <AlertTriangle size={14} />
          <span>Denúncias de Obras ({denunciasObras.length})</span>
        </Button>
      </div>

      {/* Conteúdo 0: Tags Comunitárias (Incremento 2.2) */}
      {subTab === 'TAGS' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {/* Filtros de Status da Tag */}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '10px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Filter size={14} style={{ color: 'var(--text-muted)' }} />
              <span style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>Status da Fila:</span>
              {(['PENDENTE', 'APROVADA', 'REJEITADA', 'TODAS'] as const).map((st) => (
                <Button
                  key={st}
                  variant={filtroStatusTag === st ? 'secondary' : 'ghost'}
                  size="sm"
                  onClick={() => {
                    setFiltroStatusTag(st);
                    setPaginaTags(0);
                  }}
                  style={{
                    fontSize: '12px',
                    borderColor: filtroStatusTag === st ? 'var(--brand)' : 'transparent',
                  }}
                >
                  {st === 'TODAS' ? 'Todas' : st.charAt(0) + st.slice(1).toLowerCase()}
                </Button>
              ))}
            </div>
            <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
              Página {paginaTags + 1} de {totalPaginasTags}
            </span>
          </div>

          {/* Lista de Sugestões de Tags */}
          {sugestoesTags.length === 0 ? (
            <Card style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
              Nenhuma sugestão de tag encontrada para o filtro '{filtroStatusTag.toLowerCase()}'.
            </Card>
          ) : (
            sugestoesTags.map((tag) => (
              <Card key={tag.id} style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '10px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '15px', fontWeight: 700, color: 'var(--brand-light)' }}>
                      <TagIcon size={16} />
                      <span>#{tag.nomeTag}</span>
                    </div>
                    <span style={{ fontSize: '12px', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
                      slug: {tag.slugTag}
                    </span>
                    <Badge
                      variant={
                        tag.status === 'APROVADA'
                          ? 'success'
                          : tag.status === 'REJEITADA'
                          ? 'danger'
                          : 'warning'
                      }
                      size="sm"
                    >
                      {tag.status}
                    </Badge>
                  </div>

                  <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                    Sugerido por <strong>{tag.usuarioSugeridorApelido}</strong> em {new Date(tag.dataSugestao).toLocaleString()}
                  </span>
                </div>

                <div style={{ padding: '12px 16px', borderRadius: 'var(--radius-md)', background: 'var(--bg-elevated)', fontSize: '13px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <div>
                    <strong style={{ color: 'var(--text-secondary)' }}>Obra Destino:</strong>{' '}
                    <span style={{ fontWeight: 600 }}>{tag.obraTitulo}</span>{' '}
                    <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>[ID #{tag.obraId}]</span>
                  </div>
                  {tag.justificativa && (
                    <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                      <strong>Justificativa do proponente:</strong> "{tag.justificativa}"
                    </div>
                  )}
                  {tag.motivoRejeicao && (
                    <div style={{ fontSize: '12px', color: 'var(--accent-red)', borderTop: '1px solid var(--border-subtle)', paddingTop: '6px' }}>
                      <strong>Motivo da rejeição:</strong> "{tag.motivoRejeicao}"
                    </div>
                  )}
                  {tag.adminAvaliadorApelido && (
                    <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                      Avaliado por <strong>{tag.adminAvaliadorApelido}</strong> em {tag.dataAvaliacao ? new Date(tag.dataAvaliacao).toLocaleString() : '—'}
                    </div>
                  )}
                </div>

                {tag.status === 'PENDENTE' && (
                  <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => {
                        setSelectedRejeitarTag(tag);
                        setMotivoRejeicaoTag('');
                      }}
                      style={{ borderColor: 'var(--accent-red)', color: 'var(--accent-red)' }}
                    >
                      <XCircle size={14} />
                      <span>Rejeitar...</span>
                    </Button>
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={() => handleAprovarTag(tag)}
                      isLoading={isAprovandoTagId === tag.id}
                    >
                      <CheckCircle2 size={14} />
                      <span>Aprovar Tag</span>
                    </Button>
                  </div>
                )}
              </Card>
            ))
          )}

          {/* Paginação de Tags */}
          {totalPaginasTags > 1 && (
            <div style={{ display: 'flex', justifyContent: 'center', gap: '10px', marginTop: '10px' }}>
              <Button
                variant="secondary"
                size="sm"
                disabled={paginaTags === 0}
                onClick={() => setPaginaTags((p) => Math.max(0, p - 1))}
              >
                ← Anterior
              </Button>
              <span style={{ fontSize: '12px', color: 'var(--text-secondary)', alignSelf: 'center' }}>
                Página {paginaTags + 1} de {totalPaginasTags}
              </span>
              <Button
                variant="secondary"
                size="sm"
                disabled={paginaTags + 1 >= totalPaginasTags}
                onClick={() => setPaginaTags((p) => p + 1)}
              >
                Próxima →
              </Button>
            </div>
          )}
        </div>
      )}

      {/* Conteúdo 1: Sugestões de Edição */}
      {subTab === 'SUGESTOES' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {sugestoes.length === 0 ? (
            <Card style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
              Nenhuma sugestão de edição pendente na fila.
            </Card>
          ) : (
            sugestoes.map((sug) => (
              <Card key={sug.id} style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '10px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <h3 style={{ fontSize: '15px', fontWeight: 600 }}>{sug.obraTitulo}</h3>
                    <Badge variant="warning" size="sm">Sugestão #{sug.id}</Badge>
                  </div>
                  <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                    Sugerido por <strong>{sug.usuarioApelido}</strong> em {new Date(sug.criadoEm).toLocaleDateString()}
                  </span>
                </div>

                <div style={{ padding: '14px', borderRadius: 'var(--radius-md)', background: 'var(--bg-elevated)', fontSize: '13px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {sug.tituloSugerido && (
                    <div>
                      <strong style={{ color: 'var(--text-secondary)' }}>Título Sugerido:</strong>{' '}
                      <span style={{ color: 'var(--brand-light)' }}>{sug.tituloSugerido}</span>
                    </div>
                  )}
                  {sug.sinopseSugerida && (
                    <div>
                      <strong style={{ color: 'var(--text-secondary)' }}>Sinopse Sugerida:</strong>{' '}
                      <p style={{ marginTop: '4px', color: 'var(--text-primary)', whiteSpace: 'pre-wrap' }}>{sug.sinopseSugerida}</p>
                    </div>
                  )}
                  {sug.justificativa && (
                    <div style={{ fontSize: '12px', color: 'var(--text-muted)', fontStyle: 'italic', borderTop: '1px solid var(--border-subtle)', paddingTop: '6px' }}>
                      Justificativa do usuário: "{sug.justificativa}"
                    </div>
                  )}
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      setSelectedSugestao({ id: sug.id, aprovada: false, obraTitulo: sug.obraTitulo });
                      setParecerSugestao('');
                    }}
                    style={{ borderColor: 'var(--accent-red)', color: 'var(--accent-red)' }}
                  >
                    <XCircle size={14} />
                    <span>Rejeitar</span>
                  </Button>
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => {
                      setSelectedSugestao({ id: sug.id, aprovada: true, obraTitulo: sug.obraTitulo });
                      setParecerSugestao('');
                    }}
                  >
                    <CheckCircle2 size={14} />
                    <span>Aprovar Edição</span>
                  </Button>
                </div>
              </Card>
            ))
          )}
        </div>
      )}

      {/* Conteúdo 2: Denúncias de Comentários */}
      {subTab === 'COMENTARIOS' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {denunciasComentarios.length === 0 ? (
            <Card style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
              Nenhuma denúncia de comentário ofensivo pendente.
            </Card>
          ) : (
            denunciasComentarios.map((den) => (
              <Card key={den.id} style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '10px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <h3 style={{ fontSize: '15px', fontWeight: 600 }}>Comentário #{den.comentarioId}</h3>
                    <Badge variant="danger" size="sm">Denúncia #{den.id}</Badge>
                  </div>
                  <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                    Denunciado por <strong>{den.denuncianteApelido}</strong> em {new Date(den.criadoEm).toLocaleDateString()}
                  </span>
                </div>

                <div style={{ padding: '14px', borderRadius: 'var(--radius-md)', background: 'var(--bg-elevated)', fontSize: '13px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  <div>
                    <strong style={{ color: 'var(--text-secondary)' }}>Autor do Comentário:</strong>{' '}
                    <span>{den.autorComentarioApelido}</span> <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>[ID #{den.autorComentarioId}]</span>
                  </div>
                  <div>
                    <strong style={{ color: 'var(--text-secondary)' }}>Conteúdo Reportado:</strong>
                    <blockquote style={{ marginTop: '4px', paddingLeft: '10px', borderLeft: '3px solid var(--accent-red)', color: 'var(--text-primary)', fontStyle: 'italic' }}>
                      "{den.conteudoComentario}"
                    </blockquote>
                  </div>
                  <div style={{ fontSize: '12px', color: 'var(--text-muted)', borderTop: '1px solid var(--border-subtle)', paddingTop: '6px' }}>
                    <strong>Motivo da Denúncia:</strong> {den.motivo}
                  </div>
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      setSelectedDenunciaComentario({ id: den.id, aceitar: false });
                      setParecerComentario('');
                    }}
                  >
                    <span>Rejeitar Denúncia</span>
                  </Button>
                  <Button
                    variant="danger"
                    size="sm"
                    onClick={() => {
                      setSelectedDenunciaComentario({ id: den.id, aceitar: true });
                      setParecerComentario('');
                    }}
                  >
                    <CheckCircle2 size={14} />
                    <span>Aceitar & Moderar Conteúdo</span>
                  </Button>
                </div>
              </Card>
            ))
          )}
        </div>
      )}

      {/* Conteúdo 3: Denúncias de Obras */}
      {subTab === 'OBRAS' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {denunciasObras.length === 0 ? (
            <Card style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
              Nenhuma denúncia de obra / violação de diretrizes pendente.
            </Card>
          ) : (
            denunciasObras.map((den) => (
              <Card key={den.id} style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '10px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <h3 style={{ fontSize: '15px', fontWeight: 600 }}>{den.obraTitulo}</h3>
                    <Badge variant="danger" size="sm">DMCA / Violação #{den.id}</Badge>
                  </div>
                  <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                    Denunciado por <strong>{den.denuncianteApelido}</strong> em {new Date(den.criadoEm).toLocaleDateString()}
                  </span>
                </div>

                <div style={{ padding: '14px', borderRadius: 'var(--radius-md)', background: 'var(--bg-elevated)', fontSize: '13px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  <div>
                    <strong style={{ color: 'var(--text-secondary)' }}>Obra ID:</strong> #{den.obraId}
                  </div>
                  <div>
                    <strong style={{ color: 'var(--text-secondary)' }}>Motivo / Justificativa Legal:</strong>
                    <p style={{ marginTop: '4px', color: 'var(--text-primary)' }}>{den.motivo}</p>
                  </div>
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      setSelectedDenunciaObra({ id: den.id, aceitar: false });
                      setParecerObra('');
                    }}
                  >
                    <span>Rejeitar Denúncia</span>
                  </Button>
                  <Button
                    variant="danger"
                    size="sm"
                    onClick={() => {
                      setSelectedDenunciaObra({ id: den.id, aceitar: true });
                      setParecerObra('');
                    }}
                  >
                    <CheckCircle2 size={14} />
                    <span>Aceitar Denúncia</span>
                  </Button>
                </div>
              </Card>
            ))
          )}
        </div>
      )}

      {/* MODAL 0: Rejeitar Sugestão de Tag (Incremento 2.2) */}
      {selectedRejeitarTag && (
        <Modal
          isOpen={true}
          title={`Rejeitar Sugestão de Tag: #${selectedRejeitarTag.nomeTag}`}
          onClose={() => setSelectedRejeitarTag(null)}
        >
          <form onSubmit={handleConfirmarRejeicaoTag} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
              Você está rejeitando a sugestão da tag <strong>#{selectedRejeitarTag.nomeTag}</strong> para a obra{' '}
              <strong>{selectedRejeitarTag.obraTitulo}</strong>. A justificativa será registrada na auditoria e exibida ao proponente.
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
              <label style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-secondary)' }}>
                Motivo da Rejeição (obrigatório, máx 255 caracteres):
              </label>
              <textarea
                required
                maxLength={255}
                rows={3}
                placeholder="Ex: Tag duplicada, irrelevante ou que viola as diretrizes de catalogação da obra..."
                value={motivoRejeicaoTag}
                onChange={(e) => setMotivoRejeicaoTag(e.target.value)}
                style={{
                  width: '100%',
                  padding: '10px 12px',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border-subtle)',
                  background: 'var(--bg-elevated)',
                  color: 'var(--text-primary)',
                  fontSize: '13px',
                  resize: 'vertical',
                }}
              />
              <span style={{ fontSize: '11px', color: 'var(--text-muted)', textAlign: 'right' }}>
                {motivoRejeicaoTag.length}/255 caracteres
              </span>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
              <Button type="button" variant="secondary" onClick={() => setSelectedRejeitarTag(null)}>
                Cancelar
              </Button>
              <Button type="submit" variant="danger" isLoading={isSubmittingRejeicaoTag}>
                Confirmar Rejeição
              </Button>
            </div>
          </form>
        </Modal>
      )}

      {/* MODAL 1: Avaliar Sugestão de Edição */}
      {selectedSugestao && (
        <Modal
          isOpen={true}
          title={`${selectedSugestao.aprovada ? 'Aprovar' : 'Rejeitar'} Sugestão de Edição`}
          onClose={() => setSelectedSugestao(null)}
        >
          <form onSubmit={handleConfirmarSugestao} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <p style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
              Confirmar decisão de curadoria para a obra <strong>{selectedSugestao.obraTitulo}</strong>?
            </p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
              <label style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-secondary)' }}>
                Parecer da Curadoria (opcional):
              </label>
              <textarea
                rows={3}
                placeholder="Justificativa técnica ou agradecimento ao colaborador..."
                value={parecerSugestao}
                onChange={(e) => setParecerSugestao(e.target.value)}
                style={{
                  width: '100%',
                  padding: '10px 12px',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border-subtle)',
                  background: 'var(--bg-elevated)',
                  color: 'var(--text-primary)',
                  fontSize: '13px',
                  resize: 'vertical',
                }}
              />
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
              <Button type="button" variant="secondary" onClick={() => setSelectedSugestao(null)}>
                Cancelar
              </Button>
              <Button type="submit" variant={selectedSugestao.aprovada ? 'primary' : 'danger'} isLoading={isSubmittingSugestao}>
                Confirmar {selectedSugestao.aprovada ? 'Aprovação' : 'Rejeição'}
              </Button>
            </div>
          </form>
        </Modal>
      )}

      {/* MODAL 2: Parecer Denúncia Comentário */}
      {selectedDenunciaComentario && (
        <Modal
          isOpen={true}
          title={`${selectedDenunciaComentario.aceitar ? 'Aceitar & Moderar' : 'Rejeitar'} Denúncia`}
          onClose={() => setSelectedDenunciaComentario(null)}
        >
          <form onSubmit={handleConfirmarDenunciaComentario} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <p style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
              {selectedDenunciaComentario.aceitar
                ? 'Ao aceitar a denúncia, o comentário será ocultado/removido do feed de discussões públicas.'
                : 'Ao rejeitar a denúncia, o comentário permanecerá visível.'}
            </p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
              <label style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-secondary)' }}>
                Parecer do Moderador (opcional):
              </label>
              <textarea
                rows={3}
                placeholder="Ex: Violação das diretrizes de respeito da comunidade..."
                value={parecerComentario}
                onChange={(e) => setParecerComentario(e.target.value)}
                style={{
                  width: '100%',
                  padding: '10px 12px',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border-subtle)',
                  background: 'var(--bg-elevated)',
                  color: 'var(--text-primary)',
                  fontSize: '13px',
                  resize: 'vertical',
                }}
              />
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
              <Button type="button" variant="secondary" onClick={() => setSelectedDenunciaComentario(null)}>
                Cancelar
              </Button>
              <Button type="submit" variant={selectedDenunciaComentario.aceitar ? 'danger' : 'secondary'} isLoading={isSubmittingComentario}>
                Confirmar Ação
              </Button>
            </div>
          </form>
        </Modal>
      )}

      {/* MODAL 3: Parecer Denúncia Obra */}
      {selectedDenunciaObra && (
        <Modal
          isOpen={true}
          title={`${selectedDenunciaObra.aceitar ? 'Aceitar' : 'Rejeitar'} Denúncia de Obra`}
          onClose={() => setSelectedDenunciaObra(null)}
        >
          <form onSubmit={handleConfirmarDenunciaObra} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <p style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
              {selectedDenunciaObra.aceitar
                ? 'Ao aceitar a denúncia, a obra será desoficializada ou restrita conforme o caso.'
                : 'Ao rejeitar a denúncia, a obra permanecerá com status inalterado.'}
            </p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
              <label style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-secondary)' }}>
                Parecer do Moderador / Jurídico:
              </label>
              <textarea
                rows={3}
                placeholder="Registro das averiguações feitas..."
                value={parecerObra}
                onChange={(e) => setParecerObra(e.target.value)}
                style={{
                  width: '100%',
                  padding: '10px 12px',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border-subtle)',
                  background: 'var(--bg-elevated)',
                  color: 'var(--text-primary)',
                  fontSize: '13px',
                  resize: 'vertical',
                }}
              />
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
              <Button type="button" variant="secondary" onClick={() => setSelectedDenunciaObra(null)}>
                Cancelar
              </Button>
              <Button type="submit" variant={selectedDenunciaObra.aceitar ? 'danger' : 'secondary'} isLoading={isSubmittingObra}>
                Confirmar
              </Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};
