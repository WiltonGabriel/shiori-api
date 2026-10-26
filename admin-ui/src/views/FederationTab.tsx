import React, { useEffect, useState } from 'react';
import {
  Share2,
  Smartphone,
  Plus,
  RefreshCw,
  Power,
  Trash2,
  CheckCircle2,
  XCircle,
} from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Modal } from '../components/ui/Modal';
import { AdminService } from '../services/admin.service';
import { toast } from '../components/ui/Toast';
import type { NoFederadoResponse, PareamentoResponse } from '../types/admin.types';

export const FederationTab: React.FC = () => {
  const [subTab, setSubTab] = useState<'NOS' | 'PAREAMENTOS'>('NOS');

  const [nos, setNos] = useState<NoFederadoResponse[]>([]);
  const [pareamentos, setPareamentos] = useState<PareamentoResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Modal Novo Nó
  const [isModalNoOpen, setIsModalNoOpen] = useState(false);
  const [noNome, setNoNome] = useState('');
  const [noUrlBase, setNoUrlBase] = useState('');
  const [noChave, setNoChave] = useState('');
  const [isSavingNo, setIsSavingNo] = useState(false);

  const carregarDados = async () => {
    try {
      setIsLoading(true);
      const [nosRes, parRes] = await Promise.allSettled([
        AdminService.listarNosFederados(),
        AdminService.listarPareamentos(),
      ]);

      if (nosRes.status === 'fulfilled') setNos(nosRes.value || []);
      if (parRes.status === 'fulfilled') setPareamentos(parRes.value.content || []);
    } catch {
      toast.error('Erro ao carregar dados de federação e pareamento.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    carregarDados();
  }, []);

  const handleSalvarNo = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!noNome.trim() || !noUrlBase.trim()) return;

    try {
      setIsSavingNo(true);
      const novo = await AdminService.cadastrarNoFederado({
        nome: noNome.trim(),
        urlBase: noUrlBase.trim(),
        chavePublicaMesh: noChave.trim() || undefined,
      });
      setNos((prev) => [...prev, novo]);
      toast.success(`Nó federado '${novo.nome}' cadastrado com sucesso!`);
      setIsModalNoOpen(false);
      setNoNome('');
      setNoUrlBase('');
      setNoChave('');
    } catch (err: any) {
      toast.error(err.message || 'Erro ao cadastrar nó federado.');
    } finally {
      setIsSavingNo(false);
    }
  };

  const handleToggleStatusNo = async (id: number, currentStatus: boolean, nome: string) => {
    try {
      const updated = await AdminService.alternarStatusNoFederado(id, !currentStatus);
      setNos((prev) => prev.map((n) => (n.id === id ? updated : n)));
      toast.success(`Nó '${nome}' ${!currentStatus ? 'ativado' : 'desativado'}.`);
    } catch (err: any) {
      toast.error(err.message || 'Erro ao alterar status do nó.');
    }
  };

  const handleRemoverNo = async (id: number, nome: string) => {
    try {
      await AdminService.removerNoFederado(id);
      setNos((prev) => prev.filter((n) => n.id !== id));
      toast.success(`Nó '${nome}' removido da malha.`);
    } catch (err: any) {
      toast.error(err.message || 'Erro ao remover nó.');
    }
  };

  const handleDecidirPareamento = async (id: number, aprovado: boolean) => {
    try {
      const updated = await AdminService.decidirPareamento(id, aprovado, aprovado ? 'Aprovado pelo administrador' : 'Rejeitado');
      setPareamentos((prev) => prev.map((p) => (p.id === id ? updated : p)));
      toast.success(`Pareamento ${aprovado ? 'aprovado' : 'rejeitado'} com sucesso!`);
    } catch (err: any) {
      toast.error(err.message || 'Erro ao processar pareamento.');
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Top Banner */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '22px', fontWeight: 700 }}>Federação Mesh & Pareamento de Instâncias</h2>
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginTop: '2px' }}>
            Conexão entre instâncias Shiori para sincronização descentralizada e pareamento seguro com clientes desktop/mobile.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '10px' }}>
          <Button variant="secondary" size="sm" onClick={carregarDados} isLoading={isLoading}>
            <RefreshCw size={14} />
            <span>Atualizar</span>
          </Button>
          <Button variant="primary" size="sm" onClick={() => setIsModalNoOpen(true)}>
            <Plus size={14} />
            <span>Cadastrar Nó Federado</span>
          </Button>
        </div>
      </div>

      {/* Subtabs */}
      <div style={{ display: 'flex', gap: '8px', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '12px' }}>
        <Button
          variant={subTab === 'NOS' ? 'primary' : 'ghost'}
          size="sm"
          onClick={() => setSubTab('NOS')}
        >
          <Share2 size={14} />
          <span>Nós Federados ({nos.length})</span>
        </Button>
        <Button
          variant={subTab === 'PAREAMENTOS' ? 'primary' : 'ghost'}
          size="sm"
          onClick={() => setSubTab('PAREAMENTOS')}
        >
          <Smartphone size={14} />
          <span>Pareamentos de Dispositivos ({pareamentos.length})</span>
        </Button>
      </div>

      {/* Seção 1: Nós Federados */}
      {subTab === 'NOS' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {nos.length === 0 ? (
            <Card style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
              Nenhum nó federado conectado a esta instância da Shiori API.
            </Card>
          ) : (
            nos.map((n) => (
              <Card
                key={n.id}
                style={{
                  padding: '20px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  flexWrap: 'wrap',
                  gap: '16px',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '14px', flex: 1, minWidth: '240px' }}>
                  <div style={{ padding: '10px', borderRadius: 'var(--radius-md)', background: 'var(--bg-elevated)', color: 'var(--brand-primary)' }}>
                    <Share2 size={20} />
                  </div>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span style={{ fontWeight: 600, fontSize: '15px' }}>{n.nome}</span>
                      <Badge variant={n.ativo ? 'success' : 'neutral'} size="sm">
                        {n.ativo ? 'Ativo' : 'Pausado'}
                      </Badge>
                    </div>
                    <div style={{ fontSize: '12px', color: 'var(--text-secondary)', marginTop: '2px', fontFamily: 'var(--font-mono)' }}>
                      {n.urlBase}
                    </div>
                    <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '4px' }}>
                      {n.ultimoSyncEm ? `Última sincronização em ${new Date(n.ultimoSyncEm).toLocaleString()}` : 'Aguardando primeiro handshake'}
                    </div>
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '8px' }}>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleToggleStatusNo(n.id, n.ativo, n.nome)}
                  >
                    <Power size={13} />
                    <span>{n.ativo ? 'Desativar' : 'Ativar'}</span>
                  </Button>
                  <Button
                    variant="danger"
                    size="sm"
                    onClick={() => handleRemoverNo(n.id, n.nome)}
                  >
                    <Trash2 size={13} />
                    <span>Remover</span>
                  </Button>
                </div>
              </Card>
            ))
          )}
        </div>
      )}

      {/* Seção 2: Pareamentos */}
      {subTab === 'PAREAMENTOS' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {pareamentos.length === 0 ? (
            <Card style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
              Nenhuma solicitação de pareamento pendente ou registrada.
            </Card>
          ) : (
            pareamentos.map((p) => (
              <Card
                key={p.id}
                style={{
                  padding: '20px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  flexWrap: 'wrap',
                  gap: '16px',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '14px', flex: 1, minWidth: '240px' }}>
                  <div style={{ padding: '10px', borderRadius: 'var(--radius-md)', background: 'var(--bg-elevated)', color: 'var(--brand-primary)' }}>
                    <Smartphone size={20} />
                  </div>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span style={{ fontWeight: 600, fontSize: '15px' }}>{p.nomeDispositivo || 'Cliente Shiori'}</span>
                      <Badge
                        variant={
                          p.status === 'APROVADO'
                            ? 'success'
                            : p.status === 'PENDENTE'
                            ? 'warning'
                            : 'danger'
                        }
                        size="sm"
                      >
                        {p.status}
                      </Badge>
                    </div>
                    <div style={{ fontSize: '11px', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)', marginTop: '2px' }}>
                      UUID: {p.instanciaUuid}
                    </div>
                    <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '2px' }}>
                      Solicitado em {new Date(p.criadoEm).toLocaleString()}
                    </div>
                  </div>
                </div>

                {p.status === 'PENDENTE' && (
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <Button variant="outline" size="sm" onClick={() => handleDecidirPareamento(p.id, false)}>
                      <XCircle size={14} />
                      <span>Rejeitar</span>
                    </Button>
                    <Button variant="primary" size="sm" onClick={() => handleDecidirPareamento(p.id, true)}>
                      <CheckCircle2 size={14} />
                      <span>Aprovar Pareamento</span>
                    </Button>
                  </div>
                )}
              </Card>
            ))
          )}
        </div>
      )}

      {/* Modal Novo Nó */}
      <Modal
        isOpen={isModalNoOpen}
        onClose={() => setIsModalNoOpen(false)}
        title="Cadastrar Novo Nó Federado"
        footer={
          <>
            <Button variant="ghost" onClick={() => setIsModalNoOpen(false)}>Cancelar</Button>
            <Button variant="primary" onClick={handleSalvarNo} isLoading={isSavingNo}>Conectar Nó</Button>
          </>
        }
      >
        <form onSubmit={handleSalvarNo} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <Input
            label="Nome da Instância Remota"
            placeholder="Ex: Shiori Hub São Paulo"
            value={noNome}
            onChange={(e) => setNoNome(e.target.value)}
            required
          />
          <Input
            label="URL Base da Instância (HTTPS)"
            placeholder="https://hub.shiori.exemplo.org"
            value={noUrlBase}
            onChange={(e) => setNoUrlBase(e.target.value)}
            required
          />
          <Input
            label="Chave Pública Mesh Ed25519 (Opcional)"
            placeholder="Chave pública criptográfica para handshake mútuo"
            value={noChave}
            onChange={(e) => setNoChave(e.target.value)}
          />
        </form>
      </Modal>
    </div>
  );
};
