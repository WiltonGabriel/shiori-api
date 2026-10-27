import React, { useEffect, useState } from 'react';
import { Radio, Plus, Trash2, RefreshCw } from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Input, Select } from '../components/ui/Input';
import { Modal } from '../components/ui/Modal';
import { AdminService } from '../services/admin.service';
import { toast } from '../components/ui/Toast';
import type { BroadcastResponse } from '../types/admin.types';

export const BroadcastsTab: React.FC = () => {
  const [broadcasts, setBroadcasts] = useState<BroadcastResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Modal Novo Broadcast
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [nivel, setNivel] = useState<'INFO' | 'ALERTA' | 'MANUTENCAO'>('INFO');
  const [titulo, setTitulo] = useState('');
  const [mensagem, setMensagem] = useState('');
  const [expiraEm, setExpiraEm] = useState('');
  const [isCreating, setIsCreating] = useState(false);

  const carregarBroadcasts = async () => {
    try {
      setIsLoading(true);
      const res = await AdminService.listarBroadcasts();
      setBroadcasts(res.content || []);
    } catch {
      toast.error('Erro ao listar broadcasts do sistema.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    carregarBroadcasts();
  }, []);

  const handleCriarBroadcast = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!titulo.trim() || !mensagem.trim()) return;

    try {
      setIsCreating(true);
      const novo = await AdminService.criarBroadcast({
        nivel,
        titulo: titulo.trim(),
        mensagem: mensagem.trim(),
        expiraEm: expiraEm ? new Date(expiraEm).toISOString() : null,
      });
      setBroadcasts((prev) => [novo, ...prev]);
      toast.success('Aviso global publicado com sucesso!');
      setIsModalOpen(false);
      setTitulo('');
      setMensagem('');
      setExpiraEm('');
    } catch (err: any) {
      toast.error(err.message || 'Erro ao publicar broadcast.');
    } finally {
      setIsCreating(false);
    }
  };

  const handleDesativar = async (id: number) => {
    try {
      await AdminService.desativarBroadcast(id);
      setBroadcasts((prev) => prev.map((b) => (b.id === id ? { ...b, ativo: false } : b)));
      toast.success('Aviso global desativado.');
    } catch (err: any) {
      toast.error(err.message || 'Erro ao desativar aviso.');
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Top Banner */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '22px', fontWeight: 700 }}>Avisos Globais de Sistema (Broadcast)</h2>
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginTop: '2px' }}>
            Transmita comunicados instantâneos de manutenção, atualizações ou comunicados a todos os clientes conectados.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '10px' }}>
          <Button variant="secondary" size="sm" onClick={carregarBroadcasts} isLoading={isLoading}>
            <RefreshCw size={14} />
            <span>Atualizar</span>
          </Button>
          <Button variant="primary" size="sm" onClick={() => setIsModalOpen(true)}>
            <Plus size={14} />
            <span>Novo Aviso Global</span>
          </Button>
        </div>
      </div>

      {/* Lista de Broadcasts */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        {broadcasts.length === 0 ? (
          <Card style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
            Nenhum aviso transmitido ou cadastrado no sistema.
          </Card>
        ) : (
          broadcasts.map((b) => {
            const getNivelBadge = () => {
              if (b.nivel === 'MANUTENCAO') return <Badge variant="danger">Manutenção</Badge>;
              if (b.nivel === 'ALERTA') return <Badge variant="warning">Alerta</Badge>;
              return <Badge variant="info">Informativo</Badge>;
            };

            return (
              <Card
                key={b.id}
                style={{
                  padding: '20px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  flexWrap: 'wrap',
                  gap: '16px',
                  opacity: b.ativo ? 1 : 0.6,
                }}
              >
                <div style={{ display: 'flex', alignItems: 'flex-start', gap: '14px', flex: 1, minWidth: '240px' }}>
                  <div style={{ padding: '10px', borderRadius: 'var(--radius-md)', background: 'var(--bg-elevated)', color: 'var(--brand-primary)', marginTop: '2px' }}>
                    <Radio size={20} />
                  </div>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span style={{ fontWeight: 600, fontSize: '15px' }}>{b.titulo}</span>
                      {getNivelBadge()}
                      <Badge variant={b.ativo ? 'success' : 'neutral'} size="sm">
                        {b.ativo ? 'Transmitindo' : 'Inativo'}
                      </Badge>
                    </div>
                    <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginTop: '4px' }}>
                      {b.mensagem}
                    </p>
                    <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '6px' }}>
                      Criado em {new Date(b.criadoEm).toLocaleString()}
                      {b.expiraEm ? ` • Expira em ${new Date(b.expiraEm).toLocaleString()}` : ' • Sem data de expiração'}
                    </div>
                  </div>
                </div>

                {b.ativo && (
                  <Button variant="outline" size="sm" onClick={() => handleDesativar(b.id)}>
                    <Trash2 size={14} />
                    <span>Desativar</span>
                  </Button>
                )}
              </Card>
            );
          })
        )}
      </div>

      {/* Modal Novo Broadcast */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Publicar Novo Aviso Global"
        footer={
          <>
            <Button variant="ghost" onClick={() => setIsModalOpen(false)}>Cancelar</Button>
            <Button variant="primary" onClick={handleCriarBroadcast} isLoading={isCreating}>Publicar Aviso</Button>
          </>
        }
      >
        <form onSubmit={handleCriarBroadcast} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <Select
            label="Nível de Severidade"
            value={nivel}
            onChange={(e) => setNivel(e.target.value as any)}
          >
            <option value="INFO">Informativo (Azul / Neutro)</option>
            <option value="ALERTA">Alerta Importante (Amarelo / Âmbar)</option>
            <option value="MANUTENCAO">Manutenção Crítica (Vermelho / Bloqueante)</option>
          </Select>

          <Input
            label="Título do Comunicado"
            placeholder="Ex: Manutenção programada do banco de dados às 03:00"
            value={titulo}
            onChange={(e) => setTitulo(e.target.value)}
            required
          />

          <div>
            <label style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-secondary)', display: 'block', marginBottom: '6px' }}>
              Mensagem Detalhada
            </label>
            <textarea
              rows={3}
              value={mensagem}
              onChange={(e) => setMensagem(e.target.value)}
              placeholder="Descreva a ação ou informe os usuários sobre o status..."
              required
              style={{
                width: '100%',
                background: 'var(--bg-glass-input)',
                border: '1px solid var(--border-subtle)',
                borderRadius: 'var(--radius-md)',
                padding: '9px 12px',
                color: 'var(--text-primary)',
                fontSize: '13px',
              }}
            />
          </div>

          <Input
            label="Data de Expiração Automática (Opcional)"
            type="datetime-local"
            value={expiraEm}
            onChange={(e) => setExpiraEm(e.target.value)}
          />
        </form>
      </Modal>
    </div>
  );
};
