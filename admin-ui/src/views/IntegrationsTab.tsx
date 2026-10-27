import React, { useEffect, useState } from 'react';
import {
  Cable,
  Plus,
  RefreshCw,
  Power,
  Search,
  Code,
} from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Modal } from '../components/ui/Modal';
import { AdminService } from '../services/admin.service';
import { toast } from '../components/ui/Toast';
import type { ProvedorIntegracaoResponse } from '../types/admin.types';

export const IntegrationsTab: React.FC = () => {
  const [integracoes, setIntegracoes] = useState<ProvedorIntegracaoResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Modal Novo / Editar Provedor
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalCodigo, setModalCodigo] = useState('');
  const [modalNome, setModalNome] = useState('');
  const [modalBaseUrl, setModalBaseUrl] = useState('');
  const [modalApiKey, setModalApiKey] = useState('');
  const [modalTimeout, setModalTimeout] = useState('15000');
  const [modalAtivo, setModalAtivo] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

  // Ferramenta de Teste ao Vivo
  const [testCodigo, setTestCodigo] = useState('ANILIST');
  const [testIdExterno, setTestIdExterno] = useState('1');
  const [isTesting, setIsTesting] = useState(false);
  const [testResult, setTestResult] = useState<any>(null);

  const carregarIntegracoes = async () => {
    try {
      setIsLoading(true);
      const data = await AdminService.listarIntegracoes();
      setIntegracoes(data);
    } catch {
      toast.error('Erro ao listar provedores de integração.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    carregarIntegracoes();
  }, []);

  const handleToggleStatus = async (id: number, currentStatus: boolean, nome: string) => {
    try {
      const updated = await AdminService.alternarStatusIntegracao(id, !currentStatus);
      setIntegracoes((prev) => prev.map((item) => (item.id === id ? updated : item)));
      toast.success(`Provedor '${nome}' ${!currentStatus ? 'ativado' : 'desativado'} com sucesso.`);
    } catch (err: any) {
      toast.error(err.message || 'Falha ao alterar status da integração.');
    }
  };

  const handleSalvarIntegracao = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setIsSaving(true);
      const payload = {
        codigo: modalCodigo.toUpperCase().trim(),
        nome: modalNome.trim(),
        baseUrl: modalBaseUrl.trim(),
        apiKey: modalApiKey.trim() || null,
        timeoutMs: parseInt(modalTimeout, 10) || 15000,
        ativo: modalAtivo,
      };

      const saved = await AdminService.salvarIntegracao(payload);
      toast.success(`Provedor ${saved.nome} salvo com sucesso!`);
      setIsModalOpen(false);
      setModalCodigo('');
      setModalNome('');
      setModalBaseUrl('');
      setModalApiKey('');
      carregarIntegracoes();
    } catch (err: any) {
      toast.error(err.message || 'Erro ao salvar provedor de integração.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleTestarConsulta = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!testCodigo || !testIdExterno) return;

    try {
      setIsTesting(true);
      setTestResult(null);
      const res = await AdminService.testarConsultaExterna(testCodigo, testIdExterno);
      setTestResult(res);
      toast.success('Consulta externa executada com sucesso!');
    } catch (err: any) {
      setTestResult({ erro: err.message });
      toast.error(err.message || 'Falha ao consultar obra na integração externa.');
    } finally {
      setIsTesting(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Top Banner */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '22px', fontWeight: 700 }}>Integrações Bibliográficas</h2>
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginTop: '2px' }}>
            Gerencie provedores de metadados externos (AniList, Kitsu, MangaDex, OpenLibrary) e teste consultas ao vivo.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '10px' }}>
          <Button variant="secondary" size="sm" onClick={carregarIntegracoes} isLoading={isLoading}>
            <RefreshCw size={14} />
            <span>Atualizar</span>
          </Button>
          <Button variant="primary" size="sm" onClick={() => setIsModalOpen(true)}>
            <Plus size={14} />
            <span>Novo Provedor</span>
          </Button>
        </div>
      </div>

      {/* Grid de Provedores Cadastrados */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '16px' }}>
        {integracoes.map((item) => (
          <Card key={item.id} style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '14px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <div style={{ padding: '8px', borderRadius: 'var(--radius-md)', background: item.ativo ? 'rgba(16, 185, 129, 0.15)' : 'rgba(148, 163, 184, 0.15)', color: item.ativo ? 'var(--accent-emerald)' : 'var(--text-muted)' }}>
                  <Cable size={18} />
                </div>
                <div>
                  <h3 style={{ fontSize: '15px', fontWeight: 600 }}>{item.nome}</h3>
                  <span style={{ fontSize: '11px', fontFamily: 'var(--font-mono)', color: 'var(--text-muted)' }}>
                    {item.codigo}
                  </span>
                </div>
              </div>
              <Badge variant={item.ativo ? 'success' : 'neutral'}>
                {item.ativo ? 'Ativo' : 'Inativo'}
              </Badge>
            </div>

            <div style={{ fontSize: '12px', color: 'var(--text-secondary)', display: 'flex', flexDirection: 'column', gap: '6px' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <span>URL Endpoint:</span>
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: '11px', color: 'var(--brand-light)' }}>
                  {item.baseUrl}
                </span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <span>Timeout Máximo:</span>
                <span>{item.timeoutMs} ms</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <span>Chave de API:</span>
                <span>{item.apiKey ? '••••••••' : 'Nenhuma (Pública)'}</span>
              </div>
            </div>

            <div style={{ borderTop: '1px solid var(--border-subtle)', paddingTop: '12px', display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button
                variant={item.ativo ? 'outline' : 'primary'}
                size="sm"
                onClick={() => handleToggleStatus(item.id, item.ativo, item.nome)}
              >
                <Power size={13} />
                <span>{item.ativo ? 'Desativar Provedor' : 'Ativar Provedor'}</span>
              </Button>
            </div>
          </Card>
        ))}
      </div>

      {/* Ferramenta de Teste de Consulta Externa em Tempo Real */}
      <Card style={{ padding: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
          <Code size={20} style={{ color: 'var(--brand-primary)' }} />
          <div>
            <h3 style={{ fontSize: '16px', fontWeight: 600 }}>Testador de Consulta Externa ao Vivo</h3>
            <p style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
              Execute uma busca direta pela Shiori API contra o provedor externo para validar integração e parsing de metadados.
            </p>
          </div>
        </div>

        <form onSubmit={handleTestarConsulta} style={{ display: 'flex', flexWrap: 'wrap', gap: '12px', alignItems: 'flex-end' }}>
          <div style={{ minWidth: '180px' }}>
            <label style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-secondary)', display: 'block', marginBottom: '6px' }}>
              Provedor
            </label>
            <select
              value={testCodigo}
              onChange={(e) => setTestCodigo(e.target.value)}
              style={{
                background: 'var(--bg-glass-input)',
                border: '1px solid var(--border-subtle)',
                borderRadius: 'var(--radius-md)',
                padding: '9px 12px',
                color: 'var(--text-primary)',
                fontSize: '13px',
                width: '100%',
              }}
            >
              <option value="ANILIST">AniList (GraphQL)</option>
              <option value="KITSU">Kitsu (JSON:API)</option>
              <option value="MANGADEX">MangaDex (REST v5)</option>
              <option value="OPENLIBRARY">Open Library (Books)</option>
            </select>
          </div>

          <div style={{ flex: 1, minWidth: '220px' }}>
            <Input
              label="ID Externo da Obra (Ex: 1, OL82563W, etc.)"
              value={testIdExterno}
              onChange={(e) => setTestIdExterno(e.target.value)}
              placeholder="Digite o ID externo..."
              required
            />
          </div>

          <Button type="submit" variant="primary" isLoading={isTesting} style={{ height: '38px' }}>
            <Search size={14} />
            <span>Consultar Obra</span>
          </Button>
        </form>

        {testResult && (
          <div style={{ marginTop: '16px' }}>
            <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-secondary)' }}>
              Resultado Retornado da Integração Externa:
            </span>
            <pre
              style={{
                marginTop: '8px',
                padding: '16px',
                borderRadius: 'var(--radius-md)',
                background: 'var(--bg-app)',
                border: '1px solid var(--border-subtle)',
                color: '#A5B4FC',
                fontSize: '12px',
                fontFamily: 'var(--font-mono)',
                overflowX: 'auto',
                maxHeight: '320px',
              }}
            >
              {JSON.stringify(testResult, null, 2)}
            </pre>
          </div>
        )}
      </Card>

      {/* Modal Novo Provedor */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Cadastrar / Atualizar Provedor de Metadados"
        footer={
          <>
            <Button variant="ghost" onClick={() => setIsModalOpen(false)}>Cancelar</Button>
            <Button variant="primary" onClick={handleSalvarIntegracao} isLoading={isSaving}>Salvar Provedor</Button>
          </>
        }
      >
        <form onSubmit={handleSalvarIntegracao} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <Input
            label="Código Identificador (Ex: ANILIST, GOOGLE_BOOKS)"
            value={modalCodigo}
            onChange={(e) => setModalCodigo(e.target.value)}
            required
          />
          <Input
            label="Nome Exibido"
            value={modalNome}
            onChange={(e) => setModalNome(e.target.value)}
            required
          />
          <Input
            label="URL Base da API Externa"
            value={modalBaseUrl}
            onChange={(e) => setModalBaseUrl(e.target.value)}
            required
          />
          <Input
            label="Chave de API / Token (Opcional)"
            value={modalApiKey}
            onChange={(e) => setModalApiKey(e.target.value)}
            type="password"
          />
          <Input
            label="Timeout de Requisição (ms)"
            type="number"
            value={modalTimeout}
            onChange={(e) => setModalTimeout(e.target.value)}
            required
          />
          <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', cursor: 'pointer' }}>
            <input
              type="checkbox"
              checked={modalAtivo}
              onChange={(e) => setModalAtivo(e.target.checked)}
              style={{ accentColor: 'var(--brand-primary)', cursor: 'pointer' }}
            />
            Ativar provedor imediatamente após salvar
          </label>
        </form>
      </Modal>
    </div>
  );
};
