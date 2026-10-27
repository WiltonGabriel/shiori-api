import React from 'react';
import {
  LayoutDashboard,
  Cable,
  SlidersHorizontal,
  Layers,
  ShieldAlert,
  Users,
  History,
  Radio,
  Share2,
} from 'lucide-react';

export type AdminTab =
  | 'DASHBOARD'
  | 'INTEGRACOES'
  | 'CONFIGS'
  | 'CATALOGO'
  | 'MODERACAO'
  | 'USUARIOS'
  | 'AUDITORIA'
  | 'BROADCASTS'
  | 'FEDERACAO';

interface SidebarProps {
  activeTab: AdminTab;
  onSelectTab: (tab: AdminTab) => void;
  pendingSuggestionsCount?: number;
  openReportsCount?: number;
}

export const Sidebar: React.FC<SidebarProps> = ({
  activeTab,
  onSelectTab,
  pendingSuggestionsCount = 0,
  openReportsCount = 0,
}) => {
  const menuItems: Array<{
    tab: AdminTab;
    label: string;
    icon: React.ReactNode;
    badgeCount?: number;
  }> = [
    {
      tab: 'DASHBOARD',
      label: 'Visão Geral & Métricas',
      icon: <LayoutDashboard size={18} />,
    },
    {
      tab: 'INTEGRACOES',
      label: 'Integrações Bibliográficas',
      icon: <Cable size={18} />,
    },
    {
      tab: 'CONFIGS',
      label: 'Feature Flags & Sistema',
      icon: <SlidersHorizontal size={18} />,
    },
    {
      tab: 'CATALOGO',
      label: 'Curadoria de Catálogo',
      icon: <Layers size={18} />,
    },
    {
      tab: 'MODERACAO',
      label: 'Filas de Moderação',
      icon: <ShieldAlert size={18} />,
      badgeCount: pendingSuggestionsCount + openReportsCount,
    },
    {
      tab: 'USUARIOS',
      label: 'Usuários & Permissões',
      icon: <Users size={18} />,
    },
    {
      tab: 'BROADCASTS',
      label: 'Avisos Globais (Broadcast)',
      icon: <Radio size={18} />,
    },
    {
      tab: 'FEDERACAO',
      label: 'Malha & Federação (Mesh)',
      icon: <Share2 size={18} />,
    },
    {
      tab: 'AUDITORIA',
      label: 'Trilha de Auditoria',
      icon: <History size={18} />,
    },
  ];

  return (
    <aside
      style={{
        width: 'var(--sidebar-width)',
        background: 'var(--bg-surface)',
        borderRight: '1px solid var(--border-subtle)',
        display: 'flex',
        flexDirection: 'column',
        padding: '16px 12px',
        gap: '4px',
        overflowY: 'auto',
        flexShrink: 0,
      }}
      className="admin-sidebar"
    >
      <div style={{ padding: '0 8px 12px 8px', fontSize: '11px', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.08em' }}>
        Administração do Hub
      </div>
      {menuItems.map((item) => {
        const isActive = activeTab === item.tab;
        return (
          <button
            key={item.tab}
            onClick={() => onSelectTab(item.tab)}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: '10px',
              padding: '10px 14px',
              borderRadius: 'var(--radius-md)',
              border: isActive ? '1px solid rgba(99, 102, 241, 0.4)' : '1px solid transparent',
              background: isActive ? 'rgba(99, 102, 241, 0.15)' : 'transparent',
              color: isActive ? 'var(--brand-light)' : 'var(--text-secondary)',
              fontWeight: isActive ? 600 : 500,
              fontSize: '13px',
              cursor: 'pointer',
              transition: 'all var(--transition-fast)',
              textAlign: 'left',
              width: '100%',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <span style={{ color: isActive ? 'var(--brand-primary)' : 'var(--text-muted)' }}>
                {item.icon}
              </span>
              <span>{item.label}</span>
            </div>
            {item.badgeCount && item.badgeCount > 0 ? (
              <span
                style={{
                  background: 'var(--accent-rose)',
                  color: '#FFFFFF',
                  fontSize: '10px',
                  fontWeight: 700,
                  padding: '2px 6px',
                  borderRadius: 'var(--radius-full)',
                }}
              >
                {item.badgeCount}
              </span>
            ) : null}
          </button>
        );
      })}

      <div style={{ marginTop: 'auto', paddingTop: '16px', borderTop: '1px solid var(--border-subtle)', paddingLeft: '8px', paddingRight: '8px' }}>
        <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
          <div>Shiori API Hub v0.0.16-alpha</div>
          <div style={{ marginTop: '2px' }}>Java 25 LTS & Spring Boot 4</div>
        </div>
      </div>
    </aside>
  );
};
