import React, { useEffect, useState } from 'react';
import {
  Users,
  BookOpen,
  Bookmark,
  FileQuestion,
  AlertTriangle,
  Activity,
  Server,
  RefreshCw,
  Database,
  Cpu,
} from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { AdminService } from '../services/admin.service';
import { toast } from '../components/ui/Toast';
import type {
  AdminDashboardResponse,
  HealthCheckResponse,
  AppInfoResponse,
  RegistroAuditoriaResponse,
} from '../types/admin.types';
import type { AdminTab } from '../components/layout/Sidebar';

interface DashboardTabProps {
  onNavigateTab: (tab: AdminTab) => void;
}

export const DashboardTab: React.FC<DashboardTabProps> = ({ onNavigateTab }) => {
  const [metrics, setMetrics] = useState<AdminDashboardResponse | null>(null);
  const [health, setHealth] = useState<HealthCheckResponse | null>(null);
  const [info, setInfo] = useState<AppInfoResponse | null>(null);
  const [audits, setAudits] = useState<RegistroAuditoriaResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const loadData = async () => {
    try {
      setIsLoading(true);
      const [metricsRes, healthRes, infoRes, auditRes] = await Promise.allSettled([
        AdminService.getDashboard(),
        AdminService.getHealth(),
        AdminService.getInfo(),
        AdminService.getAuditoria(0, 5),
      ]);

      if (metricsRes.status === 'fulfilled') setMetrics(metricsRes.value);
      if (healthRes.status === 'fulfilled') setHealth(healthRes.value);
      if (infoRes.status === 'fulfilled') setInfo(infoRes.value);
      if (auditRes.status === 'fulfilled') setAudits(auditRes.value.content || []);
    } catch {
      toast.error('Erro ao atualizar dados do dashboard.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Top Banner */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '22px', fontWeight: 700 }}>Visão Geral do Hub</h2>
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginTop: '2px' }}>
            Estado de saúde dos serviços, contadores em tempo real e monitoramento operacional.
          </p>
        </div>
        <Button variant="secondary" size="sm" onClick={loadData} isLoading={isLoading}>
          <RefreshCw size={14} />
          <span>Atualizar Métricas</span>
        </Button>
      </div>

      {/* Grid de Métricas Principais */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
          gap: '16px',
        }}
      >
        {/* Total Usuários */}
        <Card style={{ padding: '18px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-secondary)' }}>Usuários Cadastrados</span>
            <div style={{ padding: '8px', borderRadius: 'var(--radius-md)', background: 'rgba(99, 102, 241, 0.15)', color: 'var(--brand-primary)' }}>
              <Users size={18} />
            </div>
          </div>
          <div style={{ fontSize: '28px', fontWeight: 700, marginTop: '12px', color: 'var(--text-primary)' }}>
            {metrics ? metrics.totalUsuarios : '—'}
          </div>
          <div style={{ fontSize: '12px', color: 'var(--accent-emerald)', marginTop: '4px', display: 'flex', alignItems: 'center', gap: '4px' }}>
            <span>● {metrics ? metrics.totalUsuariosAtivos : '—'} ativos</span>
          </div>
        </Card>

        {/* Obras no Catálogo */}
        <Card style={{ padding: '18px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-secondary)' }}>Obras no Catálogo</span>
            <div style={{ padding: '8px', borderRadius: 'var(--radius-md)', background: 'rgba(6, 182, 212, 0.15)', color: 'var(--accent-cyan)' }}>
              <BookOpen size={18} />
            </div>
          </div>
          <div style={{ fontSize: '28px', fontWeight: 700, marginTop: '12px', color: 'var(--text-primary)' }}>
            {metrics ? metrics.totalObras : '—'}
          </div>
          <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '4px' }}>
            Acervo indexado no PostgreSQL
          </div>
        </Card>

        {/* Itens na Estante */}
        <Card style={{ padding: '18px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-secondary)' }}>Snapshots de Estante</span>
            <div style={{ padding: '8px', borderRadius: 'var(--radius-md)', background: 'rgba(168, 85, 247, 0.15)', color: 'var(--accent-purple)' }}>
              <Bookmark size={18} />
            </div>
          </div>
          <div style={{ fontSize: '28px', fontWeight: 700, marginTop: '12px', color: 'var(--text-primary)' }}>
            {metrics ? metrics.totalItensEstante : '—'}
          </div>
          <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '4px' }}>
            Soberania de leitura dos leitores
          </div>
        </Card>

        {/* Fila de Sugestões Pendentes */}
        <Card
          style={{
            padding: '18px',
            cursor: 'pointer',
            borderColor: metrics && metrics.totalSugestoesPendentes > 0 ? 'var(--accent-amber)' : undefined,
          }}
          onClick={() => onNavigateTab('MODERACAO')}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-secondary)' }}>Sugestões de Edição</span>
            <div style={{ padding: '8px', borderRadius: 'var(--radius-md)', background: 'rgba(245, 158, 11, 0.15)', color: 'var(--accent-amber)' }}>
              <FileQuestion size={18} />
            </div>
          </div>
          <div style={{ fontSize: '28px', fontWeight: 700, marginTop: '12px', color: 'var(--text-primary)' }}>
            {metrics ? metrics.totalSugestoesPendentes : '—'}
          </div>
          <div style={{ fontSize: '12px', color: 'var(--accent-amber)', marginTop: '4px' }}>
            Aguardando curadoria do acervo →
          </div>
        </Card>

        {/* Denúncias Abertas */}
        <Card
          style={{
            padding: '18px',
            cursor: 'pointer',
            borderColor:
              metrics && (metrics.totalDenunciasComentariosPendentes > 0 || metrics.totalDenunciasObrasPendentes > 0)
                ? 'var(--accent-rose)'
                : undefined,
          }}
          onClick={() => onNavigateTab('MODERACAO')}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-secondary)' }}>Denúncias Abertas</span>
            <div style={{ padding: '8px', borderRadius: 'var(--radius-md)', background: 'rgba(244, 63, 94, 0.15)', color: 'var(--accent-rose)' }}>
              <AlertTriangle size={18} />
            </div>
          </div>
          <div style={{ fontSize: '28px', fontWeight: 700, marginTop: '12px', color: 'var(--text-primary)' }}>
            {metrics ? metrics.totalDenunciasComentariosPendentes + metrics.totalDenunciasObrasPendentes : '—'}
          </div>
          <div style={{ fontSize: '12px', color: 'var(--accent-rose)', marginTop: '4px' }}>
            Moderação prioritária de abuso →
          </div>
        </Card>
      </div>

      {/* Painel de Saúde do Sistema e Infraestrutura */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '20px' }}>
        <Card style={{ padding: '22px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <Server size={20} style={{ color: 'var(--brand-primary)' }} />
              <h3 style={{ fontSize: '15px', fontWeight: 600 }}>Saúde dos Serviços (Actuator)</h3>
            </div>
            <Badge variant={health?.status === 'UP' ? 'success' : 'danger'}>
              {health?.status || 'DESCONHECIDO'}
            </Badge>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 12px', borderRadius: 'var(--radius-md)', background: 'var(--bg-elevated)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Database size={16} style={{ color: 'var(--text-muted)' }} />
                <span style={{ fontSize: '13px', fontWeight: 500 }}>Banco de Dados PostgreSQL</span>
              </div>
              <Badge variant="success" size="sm">Conectado</Badge>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 12px', borderRadius: 'var(--radius-md)', background: 'var(--bg-elevated)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Cpu size={16} style={{ color: 'var(--text-muted)' }} />
                <span style={{ fontSize: '13px', fontWeight: 500 }}>Runtime da API</span>
              </div>
              <span style={{ fontSize: '12px', fontFamily: 'var(--font-mono)', color: 'var(--text-secondary)' }}>
                {info?.app?.version || 'v0.0.16-alpha'} • Java 25 LTS
              </span>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 12px', borderRadius: 'var(--radius-md)', background: 'var(--bg-elevated)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Activity size={16} style={{ color: 'var(--text-muted)' }} />
                <span style={{ fontSize: '13px', fontWeight: 500 }}>Provedores de Metadados</span>
              </div>
              <Button variant="ghost" size="sm" onClick={() => onNavigateTab('INTEGRACOES')}>
                Configurar →
              </Button>
            </div>
          </div>
        </Card>

        {/* Trilha de Auditoria Recente */}
        <Card style={{ padding: '22px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
            <h3 style={{ fontSize: '15px', fontWeight: 600 }}>Últimas Ações de Auditoria</h3>
            <Button variant="ghost" size="sm" onClick={() => onNavigateTab('AUDITORIA')}>
              Ver Todas →
            </Button>
          </div>

          {audits.length === 0 ? (
            <div style={{ padding: '24px 0', textAlign: 'center', color: 'var(--text-muted)', fontSize: '13px' }}>
              Nenhum registro de auditoria gravado recentemente.
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {audits.map((a) => (
                <div
                  key={a.id}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '8px 12px',
                    borderRadius: 'var(--radius-md)',
                    background: 'var(--bg-elevated)',
                    fontSize: '12px',
                  }}
                >
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                    <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                      {a.acao}
                    </span>
                    <span style={{ color: 'var(--text-muted)', fontSize: '11px' }}>
                      Por {a.adminApelido} • {a.alvoTipo} [{a.alvoId}]
                    </span>
                  </div>
                  <span style={{ color: 'var(--text-muted)', fontSize: '11px', fontFamily: 'var(--font-mono)' }}>
                    {new Date(a.criadoEm).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                  </span>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
};
