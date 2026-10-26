import React, { useEffect, useState } from 'react';
import {
  FileQuestion,
  MessageSquare,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  RefreshCw,
} from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Modal } from '../components/ui/Modal';
import { AdminService } from '../services/admin.service';
import { toast } from '../components/ui/Toast';
import type {
  SugestaoEdicaoResponse,
  DenunciaComentarioResponse,
  DenunciaObraResponse,
} from '../types/admin.types';

export const ModerationTab: React.FC = () => {
  const [subTab, setSubTab] = useState<'SUGESTOES' | 'COMENTARIOS' | 'OBRAS'>('SUGESTOES');

  const [sugestoes, setSugestoes] = useState<SugestaoEdicaoResponse[]>([]);
  const [denunciasComentarios, setDenunciasComentarios] = useState<DenunciaComentarioResponse[]>([]);
  const [denunciasObras, setDenunciasObras] = useState<DenunciaObraResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Modal Avaliar Sugestão
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

  const carregarDados = async () => {
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
    } catch {
      toast.error('Erro ao carregar filas de moderação.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    carregarDados();
  }, []);

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
            Avaliação de edições sugeridas pela comunidade e moderação de denúncias de abuso.
          </p>
        </div>
        <Button variant="secondary" size="sm" onClick={carregarDados} isLoading={isLoading}>
          <RefreshCw size={14} />
          <span>Atualizar Filas</span>
        </Button>
      </div>

      {/* Subtabs de Navegação */}
      <div style={{ display: 'flex', gap: '8px', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '12px' }}>
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
                  >
                    <XCircle size={14} />
                    <span>Rejeitar Sugestão</span>
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
                    <span>Aprovar Sugestão</span>
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
              Nenhuma denúncia de comentário aberta no momento.
            </Card>
          ) : (
            denunciasComentarios.map((den) => (
              <Card key={den.id} style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '10px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <Badge variant="danger" size="sm">Motivo: {den.motivo}</Badge>
                    <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Denúncia #{den.id}</span>
                  </div>
                  <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                    Denunciante: {den.denuncianteApelido} • Autor: <strong>{den.autorComentarioApelido}</strong>
                  </span>
                </div>

                <div style={{ padding: '12px', borderRadius: 'var(--radius-md)', background: 'var(--bg-elevated)', borderLeft: '3px solid var(--accent-rose)' }}>
                  <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginBottom: '4px' }}>Texto do Comentário Reportado:</div>
                  <p style={{ fontSize: '13px', color: 'var(--text-primary)' }}>{den.conteudoComentario}</p>
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
                    <XCircle size={14} />
                    <span>Rejeitar Denúncia (Improcedente)</span>
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
                    <span>Aceitar e Moderar Comentário</span>
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
              Nenhuma denúncia de obra aberta no momento.
            </Card>
          ) : (
            denunciasObras.map((den) => (
              <Card key={den.id} style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '10px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <h3 style={{ fontSize: '15px', fontWeight: 600 }}>{den.obraTitulo}</h3>
                    <Badge variant="danger" size="sm">{den.motivo}</Badge>
                  </div>
                  <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                    Reportado por {den.denuncianteApelido}
                  </span>
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
                    <XCircle size={14} />
                    <span>Rejeitar</span>
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
                    <span>Aceitar Violação</span>
                  </Button>
                </div>
              </Card>
            ))
          )}
        </div>
      )}

      {/* Modal Parecer Sugestão */}
      <Modal
        isOpen={!!selectedSugestao}
        onClose={() => setSelectedSugestao(null)}
        title={selectedSugestao?.aprovada ? 'Aprovar Sugestão de Edição' : 'Rejeitar Sugestão de Edição'}
        footer={
          <>
            <Button variant="ghost" onClick={() => setSelectedSugestao(null)}>Cancelar</Button>
            <Button
              variant={selectedSugestao?.aprovada ? 'primary' : 'danger'}
              onClick={handleConfirmarSugestao}
              isLoading={isSubmittingSugestao}
            >
              Confirmar Decisão
            </Button>
          </>
        }
      >
        <form onSubmit={handleConfirmarSugestao} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
            Obra: <strong>{selectedSugestao?.obraTitulo}</strong>
          </p>
          <Input
            label="Parecer da Curadoria (Opcional para feedback ao leitor)"
            placeholder="Ex: Dados checados e validados contra fontes bibliográficas"
            value={parecerSugestao}
            onChange={(e) => setParecerSugestao(e.target.value)}
          />
        </form>
      </Modal>

      {/* Modal Parecer Denúncia Comentário */}
      <Modal
        isOpen={!!selectedDenunciaComentario}
        onClose={() => setSelectedDenunciaComentario(null)}
        title={selectedDenunciaComentario?.aceitar ? 'Aceitar e Moderar Comentário' : 'Rejeitar Denúncia de Comentário'}
        footer={
          <>
            <Button variant="ghost" onClick={() => setSelectedDenunciaComentario(null)}>Cancelar</Button>
            <Button
              variant={selectedDenunciaComentario?.aceitar ? 'danger' : 'outline'}
              onClick={handleConfirmarDenunciaComentario}
              isLoading={isSubmittingComentario}
            >
              Confirmar Decisão
            </Button>
          </>
        }
      >
        <form onSubmit={handleConfirmarDenunciaComentario} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <Input
            label="Parecer da Moderação (Gravado na Trilha de Auditoria)"
            placeholder="Ex: Violação das diretrizes de respeito da comunidade"
            value={parecerComentario}
            onChange={(e) => setParecerComentario(e.target.value)}
          />
        </form>
      </Modal>

      {/* Modal Parecer Denúncia Obra */}
      <Modal
        isOpen={!!selectedDenunciaObra}
        onClose={() => setSelectedDenunciaObra(null)}
        title={selectedDenunciaObra?.aceitar ? 'Aceitar Violação na Obra' : 'Rejeitar Denúncia de Obra'}
        footer={
          <>
            <Button variant="ghost" onClick={() => setSelectedDenunciaObra(null)}>Cancelar</Button>
            <Button
              variant={selectedDenunciaObra?.aceitar ? 'danger' : 'outline'}
              onClick={handleConfirmarDenunciaObra}
              isLoading={isSubmittingObra}
            >
              Confirmar Decisão
            </Button>
          </>
        }
      >
        <form onSubmit={handleConfirmarDenunciaObra} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <Input
            label="Parecer da Moderação"
            placeholder="Ex: Análise de conformidade e direitos de catálogo"
            value={parecerObra}
            onChange={(e) => setParecerObra(e.target.value)}
          />
        </form>
      </Modal>
    </div>
  );
};
