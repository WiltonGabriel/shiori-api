import React, { useState } from 'react';
import { ShieldCheck, AlertCircle, ArrowRight } from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Input } from '../components/ui/Input';
import { Button } from '../components/ui/Button';
import { AdminService } from '../services/admin.service';
import { authStorage } from '../services/api.service';
import { toast } from '../components/ui/Toast';
import type { AuthResponse } from '../types/admin.types';

interface LoginViewProps {
  onLoginSuccess: (user: AuthResponse) => void;
}

export const LoginView: React.FC<LoginViewProps> = ({ onLoginSuccess }) => {
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [remember, setRemember] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !senha) {
      setErrorMessage('Por favor, informe o e-mail e a senha do administrador.');
      return;
    }

    try {
      setIsLoading(true);
      setErrorMessage(null);

      const response = await AdminService.login(email, senha, remember);

      if (response.role !== 'ROLE_ADMIN') {
        throw new Error('Acesso restrito: este usuário não possui privilégios de administrador (ROLE_ADMIN).');
      }

      if (response.token) {
        authStorage.setToken(response.token, remember);
      }
      authStorage.setUser(response, remember);

      toast.success(`Bem-vindo, ${response.apelido || 'Administrador'}!`);
      onLoginSuccess(response);
    } catch (err: any) {
      const msg = err.message || 'Falha ao autenticar. Verifique suas credenciais.';
      setErrorMessage(msg);
      toast.error(msg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'radial-gradient(ellipse at top, #161B29 0%, var(--bg-app) 70%)',
        padding: '24px',
      }}
    >
      <div style={{ width: '100%', maxWidth: '420px', display: 'flex', flexDirection: 'column', gap: '24px' }}>
        {/* Brand Header */}
        <div style={{ textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px' }}>
          <div
            style={{
              width: '52px',
              height: '52px',
              borderRadius: 'var(--radius-xl)',
              background: 'linear-gradient(135deg, var(--brand-primary) 0%, var(--accent-purple) 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#FFFFFF',
              boxShadow: '0 0 30px var(--brand-glow)',
            }}
          >
            <ShieldCheck size={30} />
          </div>
          <div>
            <h1 style={{ fontSize: '26px', fontWeight: 700, letterSpacing: '-0.02em', color: 'var(--text-primary)' }}>
              Shiori API
            </h1>
            <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginTop: '4px' }}>
              Painel de Administração da Instância
            </p>
          </div>
        </div>

        {/* Card de Login */}
        <Card glow style={{ padding: '28px' }}>
          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
            {errorMessage && (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: '10px',
                  padding: '12px',
                  borderRadius: 'var(--radius-md)',
                  background: 'rgba(244, 63, 94, 0.15)',
                  border: '1px solid rgba(244, 63, 94, 0.35)',
                  color: '#FB7185',
                  fontSize: '13px',
                }}
              >
                <AlertCircle size={18} style={{ flexShrink: 0, marginTop: '2px' }} />
                <span>{errorMessage}</span>
              </div>
            )}

            <div>
              <Input
                label="E-mail Administrativo"
                type="email"
                placeholder="sys_overseer@shiori.dev"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
                required
              />
            </div>

            <div>
              <Input
                label="Senha Master"
                type="password"
                placeholder="••••••••••••"
                value={senha}
                onChange={(e) => setSenha(e.target.value)}
                autoComplete="current-password"
                required
              />
            </div>

            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '12px', color: 'var(--text-secondary)', cursor: 'pointer' }}>
                <input
                  type="checkbox"
                  checked={remember}
                  onChange={(e) => setRemember(e.target.checked)}
                  style={{ accentColor: 'var(--brand-primary)', cursor: 'pointer' }}
                />
                Lembrar esta sessão
              </label>
            </div>

            <Button
              type="submit"
              variant="primary"
              size="lg"
              isLoading={isLoading}
              style={{ width: '100%', marginTop: '6px' }}
            >
              <span>Acessar Painel</span>
              <ArrowRight size={16} />
            </Button>
          </form>
        </Card>

        {/* Rodapé Seguro */}
        <div style={{ textAlign: 'center', fontSize: '12px', color: 'var(--text-muted)' }}>
          <div>Ambiente autenticado por JWT com permissão estrita RBAC</div>
          <div style={{ marginTop: '2px' }}>Shiori Hub — Soberania do Leitor e Desacoplamento</div>
        </div>
      </div>
    </div>
  );
};
