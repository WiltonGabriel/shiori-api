import React from 'react';
import { Sun, Moon, LogOut, ShieldCheck, Activity, Menu } from 'lucide-react';
import { Button } from '../ui/Button';
import { Badge } from '../ui/Badge';
import type { AuthResponse } from '../../types/admin.types';

interface HeaderProps {
  user: AuthResponse | null;
  theme: 'dark' | 'light';
  onToggleTheme: () => void;
  onLogout: () => void;
  onToggleSidebar?: () => void;
  isApiHealthy?: boolean;
}

export const Header: React.FC<HeaderProps> = ({
  user,
  theme,
  onToggleTheme,
  onLogout,
  onToggleSidebar,
  isApiHealthy = true,
}) => {
  return (
    <header
      style={{
        height: 'var(--header-height)',
        background: 'var(--bg-glass)',
        backdropFilter: 'blur(16px)',
        WebkitBackdropFilter: 'blur(16px)',
        borderBottom: '1px solid var(--border-subtle)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '0 24px',
        position: 'sticky',
        top: 0,
        zIndex: 50,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        {onToggleSidebar && (
          <Button variant="ghost" size="sm" onClick={onToggleSidebar} style={{ padding: '6px' }} className="mobile-menu-btn">
            <Menu size={20} />
          </Button>
        )}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div
            style={{
              width: '32px',
              height: '32px',
              borderRadius: 'var(--radius-md)',
              background: 'linear-gradient(135deg, var(--brand-primary) 0%, var(--accent-purple) 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#FFFFFF',
              boxShadow: '0 0 15px var(--brand-glow)',
            }}
          >
            <ShieldCheck size={18} />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{ fontFamily: 'var(--font-display)', fontWeight: 700, fontSize: '16px', letterSpacing: '-0.02em' }}>
                Shiori API
              </span>
              <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Admin Hub</span>
            </div>
          </div>
        </div>

        <Badge variant={isApiHealthy ? 'success' : 'danger'} size="sm" style={{ marginLeft: '8px' }}>
          <Activity size={12} />
          {isApiHealthy ? 'API Online' : 'Degradada'}
        </Badge>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
        <Button
          variant="ghost"
          size="sm"
          onClick={onToggleTheme}
          title={theme === 'dark' ? 'Mudar para tema claro' : 'Mudar para tema escuro'}
          style={{ width: '36px', height: '36px', padding: 0 }}
        >
          {theme === 'dark' ? <Sun size={17} /> : <Moon size={17} />}
        </Button>

        {user && (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
              padding: '4px 12px',
              borderRadius: 'var(--radius-full)',
              background: 'var(--bg-elevated)',
              border: '1px solid var(--border-subtle)',
            }}
          >
            <div
              style={{
                width: '26px',
                height: '26px',
                borderRadius: '50%',
                background: 'var(--brand-primary)',
                color: '#FFFFFF',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '12px',
                fontWeight: 700,
              }}
            >
              {user.apelido ? user.apelido.charAt(0).toUpperCase() : 'A'}
            </div>
            <div style={{ display: 'flex', flexDirection: 'column' }}>
              <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-primary)' }}>
                {user.apelido || user.email}
              </span>
              <span style={{ fontSize: '10px', color: 'var(--brand-light)', fontWeight: 600 }}>
                {user.role}
              </span>
            </div>
          </div>
        )}

        <Button
          variant="outline"
          size="sm"
          onClick={onLogout}
          title="Encerrar sessão de administração"
          style={{ gap: '6px' }}
        >
          <LogOut size={14} />
          <span>Sair</span>
        </Button>
      </div>
    </header>
  );
};
