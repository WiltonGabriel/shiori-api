import React, { useEffect, useState } from 'react';
import {
  Search,
  Shield,
  AlertOctagon,
  RefreshCw,
  Ban,
  UserCheck,
  UserX,
  RotateCcw,
} from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Input, Select } from '../components/ui/Input';
import { Modal } from '../components/ui/Modal';
import { AdminService } from '../services/admin.service';
import { toast } from '../components/ui/Toast';
import type { AdminUsuarioResponse } from '../types/admin.types';

export const UsersTab: React.FC = () => {
  const [usuarios, setUsuarios] = useState<AdminUsuarioResponse[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [isLoading, setIsLoading] = useState(true);

  // Modal Alterar Role
  const [selectedUserRole, setSelectedUserRole] = useState<AdminUsuarioResponse | null>(null);
  const [newRole, setNewRole] = useState('ROLE_LEITOR');
  const [roleMotivo, setRoleMotivo] = useState('');
  const [isUpdatingRole, setIsUpdatingRole] = useState(false);

  // Modal Alterar Status
  const [selectedUserStatus, setSelectedUserStatus] = useState<AdminUsuarioResponse | null>(null);
  const [statusMotivo, setStatusMotivo] = useState('');
  const [isUpdatingStatus, setIsUpdatingStatus] = useState(false);

  // Modal Strike
  const [selectedUserStrike, setSelectedUserStrike] = useState<AdminUsuarioResponse | null>(null);
  const [strikeMotivo, setStrikeMotivo] = useState('');
  const [isApplyingStrike, setIsApplyingStrike] = useState(false);

  // Modal Banir
  const [selectedUserBan, setSelectedUserBan] = useState<AdminUsuarioResponse | null>(null);
  const [banMotivo, setBanMotivo] = useState('');
  const [isBanning, setIsBanning] = useState(false);

  const carregarUsuarios = async (p: number = page) => {
    try {
      setIsLoading(true);
      const res = await AdminService.listarUsuarios(p, 15);
      setUsuarios(res.content || []);
      setTotalPages(res.totalPages || 1);
    } catch {
      toast.error('Erro ao listar usuários.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    carregarUsuarios(page);
  }, [page]);

  const filteredUsers = usuarios.filter((u) => {
    if (!searchTerm) return true;
    const term = searchTerm.toLowerCase();
    return (
      (u.apelido && u.apelido.toLowerCase().includes(term)) ||
      (u.email && u.email.toLowerCase().includes(term)) ||
      u.role.toLowerCase().includes(term)
    );
  });

  const handleSalvarRole = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedUserRole || !roleMotivo.trim()) {
      toast.error('Informe o motivo da alteração de privilégio para auditoria.');
      return;
    }

    try {
      setIsUpdatingRole(true);
      const updated = await AdminService.alterarRoleUsuario(selectedUserRole.id, newRole, roleMotivo.trim());
      setUsuarios((prev) => prev.map((u) => (u.id === updated.id ? updated : u)));
      toast.success(`Papel do usuário '${updated.apelido}' atualizado para ${updated.role}!`);
      setSelectedUserRole(null);
    } catch (err: any) {
      toast.error(err.message || 'Falha ao alterar papel do usuário.');
    } finally {
      setIsUpdatingRole(false);
    }
  };

  const handleSalvarStatus = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedUserStatus || !statusMotivo.trim()) {
      toast.error('Informe a justificativa da alteração de status.');
      return;
    }

    try {
      setIsUpdatingStatus(true);
      const novoStatus = !selectedUserStatus.ativo;
      const updated = await AdminService.alterarStatusUsuario(selectedUserStatus.id, novoStatus, statusMotivo.trim());
      setUsuarios((prev) => prev.map((u) => (u.id === updated.id ? updated : u)));
      toast.success(`Usuário '${updated.apelido}' ${updated.ativo ? 'reativado' : 'suspenso'}!`);
      setSelectedUserStatus(null);
    } catch (err: any) {
      toast.error(err.message || 'Falha ao alterar status do usuário.');
    } finally {
      setIsUpdatingStatus(false);
    }
  };

  const handleSalvarStrike = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedUserStrike || !strikeMotivo.trim()) {
      toast.error('Informe o motivo formal do strike.');
      return;
    }

    try {
      setIsApplyingStrike(true);
      const updated = await AdminService.aplicarStrike(selectedUserStrike.id, strikeMotivo.trim());
      setUsuarios((prev) => prev.map((u) => (u.id === updated.id ? updated : u)));
      toast.success(`Strike aplicado ao usuário '${updated.apelido}' (Total: ${updated.totalStrikes})!`);
      setSelectedUserStrike(null);
    } catch (err: any) {
      toast.error(err.message || 'Erro ao aplicar strike.');
    } finally {
      setIsApplyingStrike(false);
    }
  };

  const handleRemoverStrike = async (usuario: AdminUsuarioResponse) => {
    if (usuario.totalStrikes <= 0) return;
    try {
      const updated = await AdminService.removerStrike(usuario.id, 'Atenuação manual da administração');
      setUsuarios((prev) => prev.map((u) => (u.id === updated.id ? updated : u)));
      toast.success(`Strike removido do usuário '${updated.apelido}'!`);
    } catch (err: any) {
      toast.error(err.message || 'Erro ao remover strike.');
    }
  };

  const handleBanirUsuario = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedUserBan || !banMotivo.trim()) {
      toast.error('O motivo do banimento definitivo é obrigatório para compliance.');
      return;
    }

    try {
      setIsBanning(true);
      await AdminService.banirUsuario(selectedUserBan.id, banMotivo.trim());
      setUsuarios((prev) => prev.filter((u) => u.id !== selectedUserBan.id));
      toast.success(`Usuário '${selectedUserBan.apelido}' banido com sucesso.`);
      setSelectedUserBan(null);
    } catch (err: any) {
      toast.error(err.message || 'Erro ao banir usuário.');
    } finally {
      setIsBanning(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Top Banner */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '22px', fontWeight: 700 }}>Gestão de Usuários & RBAC</h2>
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginTop: '2px' }}>
            Atribuição de papéis (Admin, Curador, Moderador, Leitor), strikes disciplinares e suspensões.
          </p>
        </div>
        <Button variant="secondary" size="sm" onClick={() => carregarUsuarios(page)} isLoading={isLoading}>
          <RefreshCw size={14} />
          <span>Atualizar</span>
        </Button>
      </div>

      {/* Busca */}
      <Card style={{ padding: '16px' }}>
        <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
          <Search size={16} style={{ color: 'var(--text-muted)' }} />
          <input
            placeholder="Filtrar por apelido, e-mail cifrado ou papel..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{
              background: 'transparent',
              border: 'none',
              outline: 'none',
              width: '100%',
              fontSize: '13px',
              color: 'var(--text-primary)',
            }}
          />
        </div>
      </Card>

      {/* Tabela de Usuários */}
      <Card style={{ padding: '0', overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid var(--border-subtle)', background: 'var(--bg-elevated)', color: 'var(--text-muted)', fontSize: '12px' }}>
              <th style={{ padding: '12px 16px' }}>Usuário</th>
              <th style={{ padding: '12px 16px' }}>Papel (RBAC)</th>
              <th style={{ padding: '12px 16px' }}>Status</th>
              <th style={{ padding: '12px 16px' }}>Pontos</th>
              <th style={{ padding: '12px 16px' }}>Strikes</th>
              <th style={{ padding: '12px 16px', textAlign: 'right' }}>Ações</th>
            </tr>
          </thead>
          <tbody>
            {filteredUsers.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ padding: '32px', textAlign: 'center', color: 'var(--text-muted)' }}>
                  Nenhum usuário encontrado.
                </td>
              </tr>
            ) : (
              filteredUsers.map((u) => {
                const getRoleBadgeVariant = () => {
                  if (u.role === 'ROLE_ADMIN') return 'brand';
                  if (u.role === 'ROLE_MODERADOR') return 'warning';
                  if (u.role === 'ROLE_CURADOR') return 'info';
                  return 'neutral';
                };

                return (
                  <tr key={u.id} style={{ borderBottom: '1px solid var(--border-subtle)' }}>
                    <td style={{ padding: '12px 16px' }}>
                      <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{u.apelido}</div>
                      <div style={{ fontSize: '11px', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
                        {u.email}
                      </div>
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      <Badge variant={getRoleBadgeVariant()} size="sm">
                        {u.role.replace('ROLE_', '')}
                      </Badge>
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      <Badge variant={u.ativo ? 'success' : 'danger'} size="sm">
                        {u.ativo ? 'Ativo' : 'Suspenso'}
                      </Badge>
                    </td>
                    <td style={{ padding: '12px 16px', fontFamily: 'var(--font-mono)' }}>
                      {u.pontosConfianca}
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      <span style={{ color: u.totalStrikes > 0 ? 'var(--accent-rose)' : 'var(--text-muted)', fontWeight: 600 }}>
                        {u.totalStrikes}
                      </span>
                    </td>
                    <td style={{ padding: '12px 16px', textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '6px' }}>
                        <Button
                          variant="ghost"
                          size="sm"
                          title="Alterar Papel / Permissões"
                          onClick={() => {
                            setSelectedUserRole(u);
                            setNewRole(u.role);
                            setRoleMotivo('');
                          }}
                        >
                          <Shield size={14} />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          title={u.ativo ? 'Suspender Usuário' : 'Reativar Usuário'}
                          onClick={() => {
                            setSelectedUserStatus(u);
                            setStatusMotivo('');
                          }}
                        >
                          {u.ativo ? <UserX size={14} /> : <UserCheck size={14} />}
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          title="Aplicar Strike Disciplinar"
                          onClick={() => {
                            setSelectedUserStrike(u);
                            setStrikeMotivo('');
                          }}
                        >
                          <AlertOctagon size={14} />
                        </Button>
                        {u.totalStrikes > 0 && (
                          <Button
                            variant="ghost"
                            size="sm"
                            title="Remover 1 Strike"
                            onClick={() => handleRemoverStrike(u)}
                            style={{ color: 'var(--accent-amber)' }}
                          >
                            <RotateCcw size={14} />
                          </Button>
                        )}
                        <Button
                          variant="ghost"
                          size="sm"
                          title="Banir Usuário Permanentemente"
                          onClick={() => {
                            setSelectedUserBan(u);
                            setBanMotivo('');
                          }}
                          style={{ color: 'var(--accent-rose)' }}
                        >
                          <Ban size={14} />
                        </Button>
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </Card>

      {/* Paginação */}
      {totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'center', gap: '10px' }}>
          <Button
            variant="secondary"
            size="sm"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            ← Anterior
          </Button>
          <span style={{ fontSize: '12px', color: 'var(--text-secondary)', alignSelf: 'center' }}>
            Página {page + 1} de {totalPages}
          </span>
          <Button
            variant="secondary"
            size="sm"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            Próxima →
          </Button>
        </div>
      )}

      {/* Modal Alterar Role */}
      <Modal
        isOpen={!!selectedUserRole}
        onClose={() => setSelectedUserRole(null)}
        title={`Alterar Papel: ${selectedUserRole?.apelido}`}
        footer={
          <>
            <Button variant="ghost" onClick={() => setSelectedUserRole(null)}>Cancelar</Button>
            <Button variant="primary" onClick={handleSalvarRole} isLoading={isUpdatingRole}>Salvar Papel</Button>
          </>
        }
      >
        <form onSubmit={handleSalvarRole} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <Select
            label="Novo Papel no Sistema"
            value={newRole}
            onChange={(e) => setNewRole(e.target.value)}
          >
            <option value="ROLE_LEITOR">ROLE_LEITOR (Usuário comum)</option>
            <option value="ROLE_CURADOR">ROLE_CURADOR (Aprova sugestões e curadoria)</option>
            <option value="ROLE_MODERADOR">ROLE_MODERADOR (Modera denúncias e conteúdo)</option>
            <option value="ROLE_ADMIN">ROLE_ADMIN (Acesso total)</option>
          </Select>
          <Input
            label="Justificativa da Alteração de Papel (Obrigatória)"
            placeholder="Ex: Nomeação para equipe de curadoria comunitária"
            value={roleMotivo}
            onChange={(e) => setRoleMotivo(e.target.value)}
            required
          />
        </form>
      </Modal>

      {/* Modal Alterar Status */}
      <Modal
        isOpen={!!selectedUserStatus}
        onClose={() => setSelectedUserStatus(null)}
        title={`${selectedUserStatus?.ativo ? 'Suspender' : 'Reativar'} Usuário: ${selectedUserStatus?.apelido}`}
        footer={
          <>
            <Button variant="ghost" onClick={() => setSelectedUserStatus(null)}>Cancelar</Button>
            <Button
              variant={selectedUserStatus?.ativo ? 'danger' : 'primary'}
              onClick={handleSalvarStatus}
              isLoading={isUpdatingStatus}
            >
              Confirmar Alteração
            </Button>
          </>
        }
      >
        <form onSubmit={handleSalvarStatus} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <Input
            label="Motivo da Suspensão / Reativação"
            placeholder="Ex: Suspensão preventiva por investigação de conduta"
            value={statusMotivo}
            onChange={(e) => setStatusMotivo(e.target.value)}
            required
          />
        </form>
      </Modal>

      {/* Modal Aplicar Strike */}
      <Modal
        isOpen={!!selectedUserStrike}
        onClose={() => setSelectedUserStrike(null)}
        title={`Aplicar Strike Disciplinar: ${selectedUserStrike?.apelido}`}
        footer={
          <>
            <Button variant="ghost" onClick={() => setSelectedUserStrike(null)}>Cancelar</Button>
            <Button variant="danger" onClick={handleSalvarStrike} isLoading={isApplyingStrike}>Aplicar Strike</Button>
          </>
        }
      >
        <form onSubmit={handleSalvarStrike} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
            O strike reduz os pontos de confiança do usuário e penaliza reincidências de infração.
          </p>
          <Input
            label="Motivo do Strike"
            placeholder="Ex: Comportamento tóxico ou spam de comentários"
            value={strikeMotivo}
            onChange={(e) => setStrikeMotivo(e.target.value)}
            required
          />
        </form>
      </Modal>

      {/* Modal Banir */}
      <Modal
        isOpen={!!selectedUserBan}
        onClose={() => setSelectedUserBan(null)}
        title={`Banir Permanentemente: ${selectedUserBan?.apelido}`}
        footer={
          <>
            <Button variant="ghost" onClick={() => setSelectedUserBan(null)}>Cancelar</Button>
            <Button variant="danger" onClick={handleBanirUsuario} isLoading={isBanning}>Banir Definitivamente</Button>
          </>
        }
      >
        <form onSubmit={handleBanirUsuario} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <p style={{ fontSize: '13px', color: 'var(--accent-rose)' }}>
            Atenção: Esta ação é definitiva e removerá a permissão de acesso deste usuário à Shiori API.
          </p>
          <Input
            label="Motivo Formal do Banimento"
            placeholder="Ex: Discurso de ódio reiterado e violação grave dos termos de uso"
            value={banMotivo}
            onChange={(e) => setBanMotivo(e.target.value)}
            required
          />
        </form>
      </Modal>
    </div>
  );
};
