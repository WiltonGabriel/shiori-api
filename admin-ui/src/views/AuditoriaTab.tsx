import React, { useEffect, useState } from 'react';
import { RefreshCw, Search, ShieldCheck } from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { AdminService } from '../services/admin.service';
import { toast } from '../components/ui/Toast';
import type { RegistroAuditoriaResponse } from '../types/admin.types';

export const AuditoriaTab: React.FC = () => {
  const [auditorias, setAuditorias] = useState<RegistroAuditoriaResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [isLoading, setIsLoading] = useState(true);
  const [filtroAcao, setFiltroAcao] = useState('');
  const [filtroCategoria, setFiltroCategoria] = useState<'TODAS' | 'TAGS' | 'CATALOGO' | 'USUARIOS' | 'CONFIG'>('TODAS');

  const carregarAuditoria = async (p: number = page) => {
    try {
      setIsLoading(true);
      const res = await AdminService.getAuditoria(p, 20);
      setAuditorias(res.content || []);
      setTotalPages(res.totalPages || 1);
    } catch {
      toast.error('Erro ao carregar registros de auditoria.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    carregarAuditoria(page);
  }, [page]);

  const getAcaoBadgeVariant = (acao: string): 'brand' | 'success' | 'warning' | 'danger' | 'neutral' => {
    const a = acao.toUpperCase();
    if (a.includes('APROVAD') || a.includes('OFICIALIZ') || a.includes('ATIVAD')) return 'success';
    if (a.includes('REJEITAD') || a.includes('BAN') || a.includes('SUSPENS') || a.includes('DESATIVAD')) return 'danger';
    if (a.includes('MERGE') || a.includes('MESCL') || a.includes('STRIKE')) return 'warning';
    if (a.includes('OVERRIDE') || a.includes('CONFIG') || a.includes('TAG')) return 'brand';
    return 'neutral';
  };

  const filtered = auditorias.filter((a) => {
    if (filtroCategoria === 'TAGS' && !a.acao.toLowerCase().includes('tag')) return false;
    if (filtroCategoria === 'CATALOGO' && !a.acao.toLowerCase().includes('obra') && !a.acao.toLowerCase().includes('genero')) return false;
    if (filtroCategoria === 'USUARIOS' && !a.acao.toLowerCase().includes('usuario') && !a.acao.toLowerCase().includes('role') && !a.acao.toLowerCase().includes('strike')) return false;
    if (filtroCategoria === 'CONFIG' && !a.acao.toLowerCase().includes('flag') && !a.acao.toLowerCase().includes('provedor')) return false;

    if (!filtroAcao) return true;
    const term = filtroAcao.toLowerCase();
    return (
      a.acao.toLowerCase().includes(term) ||
      a.adminApelido.toLowerCase().includes(term) ||
      a.alvoTipo.toLowerCase().includes(term) ||
      (a.detalhes && a.detalhes.toLowerCase().includes(term))
    );
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Top Banner */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <ShieldCheck size={22} style={{ color: 'var(--brand-primary)' }} />
            <h2 style={{ fontSize: '22px', fontWeight: 700 }}>Trilha de Auditoria & Compliance (LGPD / ANPD)</h2>
          </div>
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginTop: '2px' }}>
            Registro imutável de todas as ações administrativas, moderação de tags comunitárias, curadoria e compliance.
          </p>
        </div>
        <Button variant="secondary" size="sm" onClick={() => carregarAuditoria(page)} isLoading={isLoading}>
          <RefreshCw size={14} />
          <span>Atualizar Trilha</span>
        </Button>
      </div>

      {/* Filtros */}
      <Card style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
        <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
          <Search size={16} style={{ color: 'var(--text-muted)' }} />
          <input
            placeholder="Filtrar por ação, administrador, alvo ou termo do parecer..."
            value={filtroAcao}
            onChange={(e) => setFiltroAcao(e.target.value)}
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

        {/* Categorias Rápidas */}
        <div style={{ display: 'flex', gap: '8px', borderTop: '1px solid var(--border-subtle)', paddingTop: '10px', flexWrap: 'wrap' }}>
          {(['TODAS', 'TAGS', 'CATALOGO', 'USUARIOS', 'CONFIG'] as const).map((cat) => (
            <Button
              key={cat}
              variant={filtroCategoria === cat ? 'secondary' : 'ghost'}
              size="sm"
              onClick={() => setFiltroCategoria(cat)}
              style={{
                fontSize: '11px',
                borderColor: filtroCategoria === cat ? 'var(--brand)' : 'transparent',
              }}
            >
              {cat === 'TODAS'
                ? 'Todos os Registros'
                : cat === 'TAGS'
                ? 'Tags Comunitárias'
                : cat === 'CATALOGO'
                ? 'Catálogo & Obras'
                : cat === 'USUARIOS'
                ? 'Usuários'
                : 'Configurações'}
            </Button>
          ))}
        </div>
      </Card>

      {/* Tabela de Auditoria */}
      <Card style={{ padding: '0', overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid var(--border-subtle)', background: 'var(--bg-elevated)', color: 'var(--text-muted)', fontSize: '12px' }}>
              <th style={{ padding: '12px 16px' }}>Data / Hora</th>
              <th style={{ padding: '12px 16px' }}>Ação Executada</th>
              <th style={{ padding: '12px 16px' }}>Administrador</th>
              <th style={{ padding: '12px 16px' }}>Alvo</th>
              <th style={{ padding: '12px 16px' }}>IP Origem</th>
              <th style={{ padding: '12px 16px' }}>Detalhes / Justificativa</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ padding: '32px', textAlign: 'center', color: 'var(--text-muted)' }}>
                  Nenhum registro de auditoria encontrado para o filtro aplicado.
                </td>
              </tr>
            ) : (
              filtered.map((a) => (
                <tr key={a.id} style={{ borderBottom: '1px solid var(--border-subtle)' }}>
                  <td style={{ padding: '12px 16px', color: 'var(--text-muted)', whiteSpace: 'nowrap', fontFamily: 'var(--font-mono)', fontSize: '12px' }}>
                    {new Date(a.criadoEm).toLocaleString()}
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    <Badge variant={getAcaoBadgeVariant(a.acao)} size="sm">{a.acao}</Badge>
                  </td>
                  <td style={{ padding: '12px 16px', fontWeight: 600 }}>
                    {a.adminApelido} <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>[#{a.adminId}]</span>
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    <span style={{ color: 'var(--text-secondary)' }}>{a.alvoTipo}</span>{' '}
                    <span style={{ fontSize: '11px', color: 'var(--brand-light)', fontFamily: 'var(--font-mono)' }}>#{a.alvoId}</span>
                  </td>
                  <td style={{ padding: '12px 16px', fontFamily: 'var(--font-mono)', fontSize: '11px', color: 'var(--text-muted)' }}>
                    {a.enderecoIp || '127.0.0.1'}
                  </td>
                  <td style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontSize: '12px' }}>
                    {a.detalhes || '—'}
                  </td>
                </tr>
              ))
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
    </div>
  );
};
