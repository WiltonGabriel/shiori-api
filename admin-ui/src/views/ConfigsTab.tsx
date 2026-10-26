import React, { useEffect, useState } from 'react';
import { RefreshCw } from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Modal } from '../components/ui/Modal';
import { AdminService } from '../services/admin.service';
import { toast } from '../components/ui/Toast';
import type { FeatureFlagResponse } from '../types/admin.types';

export const ConfigsTab: React.FC = () => {
  const [flags, setFlags] = useState<FeatureFlagResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Modal de Alteração de Flag
  const [selectedFlag, setSelectedFlag] = useState<FeatureFlagResponse | null>(null);
  const [justificativa, setJustificativa] = useState('');
  const [isUpdating, setIsUpdating] = useState(false);

  const carregarFlags = async () => {
    try {
      setIsLoading(true);
      const data = await AdminService.listarFeatureFlags();
      setFlags(data);
    } catch {
      toast.error('Erro ao carregar configurações e feature flags.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    carregarFlags();
  }, []);

  const handleAbrirModal = (flag: FeatureFlagResponse) => {
    setSelectedFlag(flag);
    setJustificativa('');
  };

  const handleConfirmarAlteracao = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedFlag) return;
    if (!justificativa.trim()) {
      toast.error('A justificativa da alteração é obrigatória para fins de auditoria.');
      return;
    }

    try {
      setIsUpdating(true);
      const updated = await AdminService.alternarFeatureFlag(
        selectedFlag.chave,
        !selectedFlag.ativo,
        justificativa.trim()
      );
      setFlags((prev) => prev.map((f) => (f.chave === updated.chave ? updated : f)));
      toast.success(`Flag '${updated.nome}' ${updated.ativo ? 'ativada' : 'desativada'} com sucesso.`);
      setSelectedFlag(null);
    } catch (err: any) {
      toast.error(err.message || 'Falha ao alterar feature flag.');
    } finally {
      setIsUpdating(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Top Banner */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '22px', fontWeight: 700 }}>Configurações & Feature Flags</h2>
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginTop: '2px' }}>
            Controle global de funcionalidades da Shiori API. Todas as alterações exigem justificativa e são auditadas.
          </p>
        </div>
        <Button variant="secondary" size="sm" onClick={carregarFlags} isLoading={isLoading}>
          <RefreshCw size={14} />
          <span>Atualizar</span>
        </Button>
      </div>

      {/* Grid de Flags */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '16px' }}>
        {flags.map((flag) => {
          const isDanger = flag.chave === 'MODO_MANUTENCAO' && flag.ativo;
          return (
            <Card
              key={flag.id}
              style={{
                padding: '20px',
                display: 'flex',
                flexDirection: 'column',
                gap: '14px',
                borderColor: isDanger ? 'var(--accent-rose)' : undefined,
              }}
            >
              <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: '10px' }}>
                <div>
                  <h3 style={{ fontSize: '15px', fontWeight: 600 }}>{flag.nome}</h3>
                  <span style={{ fontSize: '11px', fontFamily: 'var(--font-mono)', color: 'var(--text-muted)' }}>
                    {flag.chave}
                  </span>
                </div>
                <Badge variant={flag.ativo ? (isDanger ? 'danger' : 'success') : 'neutral'}>
                  {flag.ativo ? 'Ativo' : 'Inativo'}
                </Badge>
              </div>

              <p style={{ fontSize: '13px', color: 'var(--text-secondary)', minHeight: '38px' }}>
                {flag.descricao}
              </p>

              <div style={{ borderTop: '1px solid var(--border-subtle)', paddingTop: '12px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                  {flag.atualizadoEm ? `Atualizado em ${new Date(flag.atualizadoEm).toLocaleDateString()}` : 'Configuração inicial'}
                </span>
                <Button
                  variant={flag.ativo ? 'outline' : 'primary'}
                  size="sm"
                  onClick={() => handleAbrirModal(flag)}
                >
                  {flag.ativo ? 'Desativar Flag' : 'Ativar Flag'}
                </Button>
              </div>
            </Card>
          );
        })}
      </div>

      {/* Modal de Confirmação com Justificativa */}
      <Modal
        isOpen={!!selectedFlag}
        onClose={() => setSelectedFlag(null)}
        title={selectedFlag ? `Alterar Estado: ${selectedFlag.nome}` : 'Alterar Feature Flag'}
        footer={
          <>
            <Button variant="ghost" onClick={() => setSelectedFlag(null)}>Cancelar</Button>
            <Button
              variant={selectedFlag && !selectedFlag.ativo ? 'primary' : 'danger'}
              onClick={handleConfirmarAlteracao}
              isLoading={isUpdating}
            >
              {selectedFlag && !selectedFlag.ativo ? 'Confirmar Ativação' : 'Confirmar Desativação'}
            </Button>
          </>
        }
      >
        {selectedFlag && (
          <form onSubmit={handleConfirmarAlteracao} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            <div style={{ padding: '12px', borderRadius: 'var(--radius-md)', background: 'var(--bg-elevated)', fontSize: '13px' }}>
              <div><strong>Flag:</strong> {selectedFlag.nome} (<code>{selectedFlag.chave}</code>)</div>
              <div style={{ marginTop: '4px' }}>
                <strong>Novo Estado:</strong>{' '}
                <span style={{ color: !selectedFlag.ativo ? 'var(--accent-emerald)' : 'var(--accent-rose)', fontWeight: 600 }}>
                  {!selectedFlag.ativo ? 'ATIVADO' : 'DESATIVADO'}
                </span>
              </div>
            </div>

            <Input
              label="Justificativa Operacional (Obrigatório para Auditoria ANPD / Sistema)"
              placeholder="Ex: Manutenção preventiva programada da infraestrutura de dados"
              value={justificativa}
              onChange={(e) => setJustificativa(e.target.value)}
              required
            />
          </form>
        )}
      </Modal>
    </div>
  );
};
