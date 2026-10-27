import React, { useState, useEffect } from 'react';
import { Header } from './components/layout/Header';
import { Sidebar, type AdminTab } from './components/layout/Sidebar';
import { ToastContainer, toast } from './components/ui/Toast';
import { LoginView } from './views/LoginView';
import { DashboardTab } from './views/DashboardTab';
import { IntegrationsTab } from './views/IntegrationsTab';
import { ConfigsTab } from './views/ConfigsTab';
import { CatalogTab } from './views/CatalogTab';
import { ModerationTab } from './views/ModerationTab';
import { UsersTab } from './views/UsersTab';
import { BroadcastsTab } from './views/BroadcastsTab';
import { FederationTab } from './views/FederationTab';
import { AuditoriaTab } from './views/AuditoriaTab';
import { authStorage } from './services/api.service';
import { AdminService } from './services/admin.service';
import type { AuthResponse, AdminDashboardResponse } from './types/admin.types';
import './styles/tokens.css';

export const App: React.FC = () => {
  const [theme, setTheme] = useState<'dark' | 'light'>(() => {
    return (localStorage.getItem('shiori_admin_theme') as 'dark' | 'light') || 'dark';
  });

  const [user, setUser] = useState<AuthResponse | null>(() => authStorage.getUser());
  const [activeTab, setActiveTab] = useState<AdminTab>('DASHBOARD');
  const [metrics, setMetrics] = useState<AdminDashboardResponse | null>(null);
  const [, setIsSidebarOpen] = useState(false);
  const [isCheckingAuth, setIsCheckingAuth] = useState(true);

  // Sincroniza tema no DOM
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('shiori_admin_theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme((prev) => (prev === 'dark' ? 'light' : 'dark'));
  };

  // Validação inicial da sessão
  useEffect(() => {
    const verificarSessao = async () => {
      const token = authStorage.getToken();
      if (!token) {
        setUser(null);
        setIsCheckingAuth(false);
        return;
      }

      try {
        const me = await AdminService.getMe();
        if (me.role !== 'ROLE_ADMIN') {
          throw new Error('Acesso negado: Requer privilégio ROLE_ADMIN.');
        }
        setUser(me);
        authStorage.setUser(me);
      } catch {
        authStorage.clear();
        setUser(null);
      } finally {
        setIsCheckingAuth(false);
      }
    };

    verificarSessao();

    const handleExpired = () => {
      authStorage.clear();
      setUser(null);
      toast.error('Sua sessão de administrador expirou. Por favor, faça login novamente.');
    };

    window.addEventListener('shiori:auth_expired', handleExpired);
    return () => window.removeEventListener('shiori:auth_expired', handleExpired);
  }, []);

  // Carrega contadores para badges da sidebar
  useEffect(() => {
    if (user) {
      AdminService.getDashboard()
        .then((data) => setMetrics(data))
        .catch(() => {});
    }
  }, [user, activeTab]);

  const handleLogout = () => {
    authStorage.clear();
    setUser(null);
    toast.info('Sessão encerrada com sucesso.');
  };

  if (isCheckingAuth) {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--bg-app)' }}>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px' }}>
          <div style={{ width: '32px', height: '32px', border: '3px solid var(--brand-primary)', borderRightColor: 'transparent', borderRadius: '50%', animation: 'spin 0.6s linear infinite' }} />
          <span style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>Carregando Shiori Hub...</span>
        </div>
      </div>
    );
  }

  if (!user) {
    return (
      <>
        <LoginView onLoginSuccess={(u) => setUser(u)} />
        <ToastContainer />
      </>
    );
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', background: 'var(--bg-app)' }}>
      {/* Header Fixo */}
      <Header
        user={user}
        theme={theme}
        onToggleTheme={toggleTheme}
        onLogout={handleLogout}
        onToggleSidebar={() => setIsSidebarOpen((prev) => !prev)}
      />

      <div style={{ display: 'flex', flex: 1, position: 'relative' }}>
        {/* Sidebar */}
        <Sidebar
          activeTab={activeTab}
          onSelectTab={(tab) => {
            setActiveTab(tab);
            setIsSidebarOpen(false);
          }}
          pendingSuggestionsCount={metrics?.totalSugestoesPendentes || 0}
          openReportsCount={
            (metrics?.totalDenunciasComentariosPendentes || 0) +
            (metrics?.totalDenunciasObrasPendentes || 0)
          }
        />

        {/* Conteúdo Principal da Aba */}
        <main
          style={{
            flex: 1,
            padding: '28px',
            maxWidth: '1300px',
            width: '100%',
            margin: '0 auto',
            overflowY: 'auto',
          }}
        >
          {activeTab === 'DASHBOARD' && <DashboardTab onNavigateTab={(t) => setActiveTab(t)} />}
          {activeTab === 'INTEGRACOES' && <IntegrationsTab />}
          {activeTab === 'CONFIGS' && <ConfigsTab />}
          {activeTab === 'CATALOGO' && <CatalogTab />}
          {activeTab === 'MODERACAO' && <ModerationTab />}
          {activeTab === 'USUARIOS' && <UsersTab />}
          {activeTab === 'BROADCASTS' && <BroadcastsTab />}
          {activeTab === 'FEDERACAO' && <FederationTab />}
          {activeTab === 'AUDITORIA' && <AuditoriaTab />}
        </main>
      </div>

      <ToastContainer />
    </div>
  );
};
