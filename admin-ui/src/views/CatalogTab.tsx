import React, { useEffect, useState } from 'react';
import {
  Search,
  GitMerge,
  ShieldCheck,
  Edit3,
  Tags,
  Plus,
  RefreshCw,
  BookOpen,
  Filter,
  Tag as TagIcon,
  X,
  Trash2,
  Edit2,
} from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Modal } from '../components/ui/Modal';
import { AdminService } from '../services/admin.service';
import { toast } from '../components/ui/Toast';
import type { ObraResponse, GeneroResponse, TagObraResponse } from '../types/admin.types';

export const CatalogTab: React.FC = () => {
  const [obras, setObras] = useState<ObraResponse[]>([]);
  const [generos, setGeneros] = useState<GeneroResponse[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [isLoading, setIsLoading] = useState(true);

  // Filtros Avançados de Busca (Criteria API)
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);
  const [filtroTipo, setFiltroTipo] = useState<string>('');
  const [filtroStatus, setFiltroStatus] = useState<string>('');
  const [filtroOrdenacao, setFiltroOrdenacao] = useState<'NOTA_DESC' | 'NOTA_ASC' | 'CAPITULOS_DESC' | 'TITULO_ASC' | 'TITULO_DESC' | 'RECENTES'>('NOTA_DESC');

  // Modal Tags Comunitárias da Obra (Incremento 2.2)
  const [selectedObraTags, setSelectedObraTags] = useState<{ obra: ObraResponse; tags: TagObraResponse[] } | null>(null);
  const [isLoadingObraTags, setIsLoadingObraTags] = useState(false);

  // Modal Mesclar Obras
  const [isMergeModalOpen, setIsMergeModalOpen] = useState(false);
  const [idPrincipal, setIdPrincipal] = useState('');
  const [idDuplicada, setIdDuplicada] = useState('');
  const [isMerging, setIsMerging] = useState(false);

  // Modal Override Obra
  const [selectedObraOverride, setSelectedObraOverride] = useState<ObraResponse | null>(null);
  const [overrideTitulo, setOverrideTitulo] = useState('');
  const [overrideAutor, setOverrideAutor] = useState('');
  const [overrideSinopse, setOverrideSinopse] = useState('');
  const [overrideStatus, setOverrideStatus] = useState('');
  const [overrideMotivo, setOverrideMotivo] = useState('');
  const [isOverriding, setIsOverriding] = useState(false);

  // Modal Novo Gênero
  const [isGenreModalOpen, setIsGenreModalOpen] = useState(false);
  const [editingGenreId, setEditingGenreId] = useState<number | null>(null);
  const [newGenreNome, setNewGenreNome] = useState('');
  const [newGenreDescricao, setNewGenreDescricao] = useState('');
  const [isSavingGenre, setIsSavingGenre] = useState(false);
  const [isDeletingGenreId, setIsDeletingGenreId] = useState<number | null>(null);

  // Modal Mesclar Tags
  const [isMergeTagModalOpen, setIsMergeTagModalOpen] = useState(false);
  const [tagOrigemId, setTagOrigemId] = useState('');
  const [tagDestinoId, setTagDestinoId] = useState('');
  const [tagMotivo, setTagMotivo] = useState('');
  const [isMergingTags, setIsMergingTags] = useState(false);

  const carregarObras = async (p: number = page) => {
    try {
      setIsLoading(true);
      const isAdvancedActive = Boolean(filtroTipo || filtroStatus || filtroOrdenacao !== 'NOTA_DESC');

      if (isAdvancedActive) {
        const res = await AdminService.buscarObrasAvancada({
          termo: searchQuery.trim() || undefined,
          tipos: filtroTipo ? [filtroTipo] : undefined,
          status: filtroStatus ? [filtroStatus] : undefined,
          ordenacao: filtroOrdenacao,
          pagina: p,
          itensPorPagina: 15,
        });
        setObras(res.content || []);
        setTotalPages(res.totalPages || 1);
      } else {
        const res = await AdminService.buscarObras(searchQuery.trim(), p, 15);
        setObras(res.content || []);
        setTotalPages(res.totalPages || 1);
      }
    } catch {
      toast.error('Erro ao buscar obras no catálogo.');
    } finally {
      setIsLoading(false);
    }
  };

  const carregarGeneros = async () => {
    try {
      const data = await AdminService.listarGeneros();
      setGeneros(data);
    } catch {
      // silencioso
    }
  };

  useEffect(() => {
    carregarObras(page);
  }, [page, filtroTipo, filtroStatus, filtroOrdenacao]);

  useEffect(() => {
    carregarGeneros();
  }, []);

  const handleBuscar = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    carregarObras(0);
  };

  const handleLimparFiltros = () => {
    setSearchQuery('');
    setFiltroTipo('');
    setFiltroStatus('');
    setFiltroOrdenacao('NOTA_DESC');
    setPage(0);
  };

  const handleVerTagsDaObra = async (obra: ObraResponse) => {
    try {
      setIsLoadingObraTags(true);
      setSelectedObraTags({ obra, tags: [] });
      const tags = await AdminService.listarTagsDaObra(obra.id);
      setSelectedObraTags({ obra, tags });
    } catch (err: any) {
      toast.error(err.message || 'Erro ao carregar tags comunitárias da obra.');
    } finally {
      setIsLoadingObraTags(false);
    }
  };

  const handleOficializar = async (id: number, titulo: string) => {
    try {
      const updated = await AdminService.oficializarObra(id);
      setObras((prev) => prev.map((o) => (o.id === id ? updated : o)));
      toast.success(`Obra '${titulo}' oficializada no catálogo!`);
    } catch (err: any) {
      toast.error(err.message || 'Erro ao oficializar obra.');
    }
  };

  const handleMesclarObras = async (e: React.FormEvent) => {
    e.preventDefault();
    const pId = parseInt(idPrincipal, 10);
    const dId = parseInt(idDuplicada, 10);
    if (!pId || !dId || pId === dId) {
      toast.error('Informe IDs válidos e distintos para obra principal e duplicada.');
      return;
    }

    try {
      setIsMerging(true);
      await AdminService.mesclarObras(pId, dId);
      toast.success('Obras mescladas com sucesso! Snapshots de leitores foram preservados.');
      setIsMergeModalOpen(false);
      setIdPrincipal('');
      setIdDuplicada('');
      carregarObras(page);
    } catch (err: any) {
      toast.error(err.message || 'Falha ao mesclar obras.');
    } finally {
      setIsMerging(false);
    }
  };

  const handleAbrirOverride = (obra: ObraResponse) => {
    setSelectedObraOverride(obra);
    setOverrideTitulo(obra.titulo || '');
    setOverrideAutor(obra.autor || '');
    setOverrideSinopse(obra.sinopse || '');
    setOverrideStatus(obra.status || '');
    setOverrideMotivo('');
  };

  const handleSalvarOverride = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedObraOverride) return;
    if (!overrideMotivo.trim()) {
      toast.error('A justificativa do override é obrigatória.');
      return;
    }

    try {
      setIsOverriding(true);
      const updated = await AdminService.overrideObra(selectedObraOverride.id, {
        titulo: overrideTitulo.trim() || undefined,
        autor: overrideAutor.trim() || undefined,
        sinopse: overrideSinopse.trim() || undefined,
        statusObra: overrideStatus || undefined,
        motivoOverride: overrideMotivo.trim(),
      });
      setObras((prev) => prev.map((o) => (o.id === updated.id ? updated : o)));
      toast.success('Override administrativo aplicado com sucesso!');
      setSelectedObraOverride(null);
    } catch (err: any) {
      toast.error(err.message || 'Falha ao aplicar override na obra.');
    } finally {
      setIsOverriding(false);
    }
  };

  const handleCriarOuAtualizarGenero = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newGenreNome.trim()) return;

    try {
      setIsSavingGenre(true);
      if (editingGenreId) {
        const atualizado = await AdminService.atualizarGenero(
          editingGenreId,
          newGenreNome.trim(),
          newGenreDescricao.trim() || undefined
        );
        setGeneros((prev) => prev.map((g) => (g.id === editingGenreId ? atualizado : g)));
        toast.success(`Gênero '${atualizado.nome}' atualizado com sucesso!`);
        setEditingGenreId(null);
      } else {
        const novo = await AdminService.criarGenero(newGenreNome.trim(), newGenreDescricao.trim() || undefined);
        setGeneros((prev) => [...prev, novo]);
        toast.success(`Gênero '${novo.nome}' criado com sucesso!`);
      }
      setNewGenreNome('');
      setNewGenreDescricao('');
    } catch (err: any) {
      toast.error(err.message || 'Erro ao salvar gênero.');
    } finally {
      setIsSavingGenre(false);
    }
  };

  const handleIniciarEdicaoGenero = (g: GeneroResponse) => {
    setEditingGenreId(g.id);
    setNewGenreNome(g.nome);
    setNewGenreDescricao(g.descricao || '');
  };

  const handleCancelarEdicaoGenero = () => {
    setEditingGenreId(null);
    setNewGenreNome('');
    setNewGenreDescricao('');
  };

  const handleExcluirGenero = async (id: number, nome: string) => {
    if (!window.confirm(`Deseja realmente remover o gênero '${nome}'?`)) return;
    try {
      setIsDeletingGenreId(id);
      await AdminService.excluirGenero(id);
      setGeneros((prev) => prev.filter((g) => g.id !== id));
      if (editingGenreId === id) {
        handleCancelarEdicaoGenero();
      }
      toast.success(`Gênero '${nome}' removido com sucesso.`);
    } catch (err: any) {
      toast.error(err.message || 'Erro ao remover gênero.');
    } finally {
      setIsDeletingGenreId(null);
    }
  };

  const handleMesclarTags = async (e: React.FormEvent) => {
    e.preventDefault();
    const origId = parseInt(tagOrigemId, 10);
    const destId = parseInt(tagDestinoId, 10);
    if (!origId || !destId || origId === destId) {
      toast.error('Informe IDs válidos e distintos para tag de origem e destino.');
      return;
    }
    if (!tagMotivo.trim()) {
      toast.error('Informe o motivo da consolidação de tags.');
      return;
    }

    try {
      setIsMergingTags(true);
      await AdminService.mesclarTags(origId, destId, tagMotivo.trim());
      toast.success('Tags comunitárias consolidadas com sucesso!');
      setIsMergeTagModalOpen(false);
      setTagOrigemId('');
      setTagDestinoId('');
      setTagMotivo('');
    } catch (err: any) {
      toast.error(err.message || 'Erro ao mesclar tags comunitárias.');
    } finally {
      setIsMergingTags(false);
    }
  };

  const hasActiveFilters = Boolean(filtroTipo || filtroStatus || filtroOrdenacao !== 'NOTA_DESC');

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Top Banner */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '22px', fontWeight: 700 }}>Curadoria do Catálogo & Taxonomia</h2>
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginTop: '2px' }}>
            Gerenciamento do acervo oficial ({generos.length} gêneros cadastrados), mesclagem de duplicatas, tags e overrides.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          <Button variant="secondary" size="sm" onClick={() => carregarObras(page)} isLoading={isLoading}>
            <RefreshCw size={14} />
            <span>Atualizar</span>
          </Button>
          <Button variant="secondary" size="sm" onClick={() => setIsMergeTagModalOpen(true)}>
            <Tags size={14} />
            <span>Mesclar Tags</span>
          </Button>
          <Button variant="secondary" size="sm" onClick={() => setIsGenreModalOpen(true)}>
            <Plus size={14} />
            <span>Gêneros ({generos.length})</span>
          </Button>
          <Button variant="primary" size="sm" onClick={() => setIsMergeModalOpen(true)}>
            <GitMerge size={14} />
            <span>Mesclar Obras Duplicadas</span>
          </Button>
        </div>
      </div>

      {/* Barra de Busca e Filtros Avançados */}
      <Card style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
        <form onSubmit={handleBuscar} style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
          <div style={{ flex: 1 }}>
            <Input
              placeholder="Buscar obras por título no catálogo..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
          <Button
            type="button"
            variant={showAdvancedFilters || hasActiveFilters ? 'secondary' : 'ghost'}
            size="md"
            onClick={() => setShowAdvancedFilters(!showAdvancedFilters)}
            style={{
              borderColor: hasActiveFilters ? 'var(--brand)' : undefined,
              position: 'relative',
            }}
          >
            <Filter size={14} />
            <span>Filtros {hasActiveFilters ? '(Ativos)' : ''}</span>
          </Button>
          <Button type="submit" variant="primary">
            <Search size={14} />
            <span>Buscar</span>
          </Button>
        </form>

        {/* Painel Expansível de Filtros Avançados */}
        {showAdvancedFilters && (
          <div
            style={{
              paddingTop: '12px',
              borderTop: '1px solid var(--border-subtle)',
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
              gap: '12px',
              alignItems: 'end',
            }}
          >
            <div>
              <label style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>
                Formato / Tipo:
              </label>
              <select
                value={filtroTipo}
                onChange={(e) => {
                  setFiltroTipo(e.target.value);
                  setPage(0);
                }}
                style={{
                  width: '100%',
                  padding: '8px 12px',
                  borderRadius: 'var(--radius-md)',
                  background: 'var(--bg-elevated)',
                  border: '1px solid var(--border-subtle)',
                  color: 'var(--text-primary)',
                  fontSize: '13px',
                }}
              >
                <option value="">Todos os Formatos</option>
                <option value="MANGA">Mangá</option>
                <option value="MANHWA">Manhwa</option>
                <option value="MANHUA">Manhua</option>
                <option value="LIGHT_NOVEL">Light Novel</option>
                <option value="WEBTOON">Webtoon</option>
                <option value="LIVRO">Livro</option>
              </select>
            </div>

            <div>
              <label style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>
                Status de Publicação:
              </label>
              <select
                value={filtroStatus}
                onChange={(e) => {
                  setFiltroStatus(e.target.value);
                  setPage(0);
                }}
                style={{
                  width: '100%',
                  padding: '8px 12px',
                  borderRadius: 'var(--radius-md)',
                  background: 'var(--bg-elevated)',
                  border: '1px solid var(--border-subtle)',
                  color: 'var(--text-primary)',
                  fontSize: '13px',
                }}
              >
                <option value="">Todos os Status</option>
                <option value="LANCANDO">Lançando</option>
                <option value="CONCLUIDO">Concluído</option>
                <option value="PAUSADO">Pausado</option>
                <option value="CANCELADO">Cancelado</option>
                <option value="HIATO">Hiato</option>
              </select>
            </div>

            <div>
              <label style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-secondary)', display: 'block', marginBottom: '4px' }}>
                Ordenação:
              </label>
              <select
                value={filtroOrdenacao}
                onChange={(e) => {
                  setFiltroOrdenacao(e.target.value as any);
                  setPage(0);
                }}
                style={{
                  width: '100%',
                  padding: '8px 12px',
                  borderRadius: 'var(--radius-md)',
                  background: 'var(--bg-elevated)',
                  border: '1px solid var(--border-subtle)',
                  color: 'var(--text-primary)',
                  fontSize: '13px',
                }}
              >
                <option value="NOTA_DESC">Melhor Avaliação</option>
                <option value="NOTA_ASC">Menor Avaliação</option>
                <option value="TITULO_ASC">Título (A-Z)</option>
                <option value="TITULO_DESC">Título (Z-A)</option>
                <option value="RECENTES">Adicionados Recentemente</option>
                <option value="CAPITULOS_DESC">Mais Capítulos</option>
              </select>
            </div>

            {hasActiveFilters && (
              <div>
                <Button variant="ghost" size="sm" onClick={handleLimparFiltros} style={{ width: '100%' }}>
                  <X size={14} />
                  <span>Limpar Filtros</span>
                </Button>
              </div>
            )}
          </div>
        )}
      </Card>

      {/* Lista de Obras */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
        {obras.length === 0 ? (
          <Card style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
            Nenhuma obra encontrada para o filtro informado.
          </Card>
        ) : (
          obras.map((obra) => (
            <Card
              key={obra.id}
              style={{
                padding: '16px 20px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                flexWrap: 'wrap',
                gap: '16px',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '14px', flex: 1, minWidth: '240px' }}>
                <div style={{ padding: '10px', borderRadius: 'var(--radius-md)', background: 'var(--bg-elevated)', color: 'var(--brand-primary)' }}>
                  <BookOpen size={20} />
                </div>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span style={{ fontWeight: 600, fontSize: '15px' }}>{obra.titulo}</span>
                    <Badge variant={obra.oficial ? 'brand' : 'neutral'} size="sm">
                      {obra.oficial ? 'Oficial' : 'Comunitária'}
                    </Badge>
                    <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>#{obra.id}</span>
                  </div>
                  <div style={{ fontSize: '12px', color: 'var(--text-secondary)', marginTop: '2px' }}>
                    Autor: {obra.autor || 'Desconhecido'} • Tipo: {obra.tipo} • Status: {obra.status}
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => handleVerTagsDaObra(obra)}
                  title="Consultar tags comunitárias e pontuação de relevância"
                >
                  <TagIcon size={14} />
                  <span>Tags Comunitárias</span>
                </Button>
                {!obra.oficial && (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleOficializar(obra.id, obra.titulo)}
                  >
                    <ShieldCheck size={14} />
                    <span>Oficializar</span>
                  </Button>
                )}
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => handleAbrirOverride(obra)}
                >
                  <Edit3 size={14} />
                  <span>Override</span>
                </Button>
              </div>
            </Card>
          ))
        )}
      </div>

      {/* Paginação */}
      {totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'center', gap: '10px', marginTop: '12px' }}>
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

      {/* MODAL: Visualizar Tags Comunitárias da Obra (Incremento 2.2) */}
      {selectedObraTags && (
        <Modal
          isOpen={true}
          onClose={() => setSelectedObraTags(null)}
          title={`Tags Comunitárias: ${selectedObraTags.obra.titulo}`}
          footer={
            <Button variant="secondary" onClick={() => setSelectedObraTags(null)}>
              Fechar
            </Button>
          }
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            <p style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
              Tags atribuídas pela comunidade de leitores e validadas por pontuação de relevância (score de votos positivos e negativos).
            </p>

            {isLoadingObraTags ? (
              <div style={{ padding: '24px', textAlign: 'center', color: 'var(--text-muted)' }}>
                Carregando tags da obra...
              </div>
            ) : selectedObraTags.tags.length === 0 ? (
              <div style={{ padding: '24px', textAlign: 'center', color: 'var(--text-muted)', background: 'var(--bg-elevated)', borderRadius: 'var(--radius-md)' }}>
                Nenhuma tag comunitária vinculada a esta obra até o momento. Leitores podem sugerir tags pelo catálogo.
              </div>
            ) : (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                {selectedObraTags.tags.map((t) => (
                  <div
                    key={t.tagId}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                      padding: '6px 12px',
                      borderRadius: 'var(--radius-md)',
                      background: 'var(--bg-elevated)',
                      border: '1px solid var(--border-subtle)',
                      fontSize: '13px',
                    }}
                  >
                    <span style={{ fontWeight: 600, color: 'var(--brand-light)' }}>#{t.nome}</span>
                    <span style={{ fontSize: '11px', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
                      ({t.slug})
                    </span>
                    <Badge
                      variant={t.score > 0 ? 'success' : t.score < 0 ? 'danger' : 'neutral'}
                      size="sm"
                    >
                      Score: {t.score > 0 ? `+${t.score}` : t.score}
                    </Badge>
                  </div>
                ))}
              </div>
            )}
          </div>
        </Modal>
      )}

      {/* Modal Mesclar Obras */}
      <Modal
        isOpen={isMergeModalOpen}
        onClose={() => setIsMergeModalOpen(false)}
        title="Mesclar Obras Duplicadas"
        footer={
          <>
            <Button variant="ghost" onClick={() => setIsMergeModalOpen(false)}>Cancelar</Button>
            <Button variant="primary" onClick={handleMesclarObras} isLoading={isMerging}>Confirmar Mesclagem</Button>
          </>
        }
      >
        <form onSubmit={handleMesclarObras} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
            A mesclagem unifica metadados, comentários e tags na obra principal. Os snapshots de leitores que continham a obra duplicada são atualizados de forma atômica e segura.
          </p>
          <Input
            label="ID da Obra Principal (Que será mantida)"
            type="number"
            placeholder="Ex: 10"
            value={idPrincipal}
            onChange={(e) => setIdPrincipal(e.target.value)}
            required
          />
          <Input
            label="ID da Obra Duplicada (Que será absorvida)"
            type="number"
            placeholder="Ex: 42"
            value={idDuplicada}
            onChange={(e) => setIdDuplicada(e.target.value)}
            required
          />
        </form>
      </Modal>

      {/* Modal Override de Obra */}
      <Modal
        isOpen={!!selectedObraOverride}
        onClose={() => setSelectedObraOverride(null)}
        title={selectedObraOverride ? `Override Administrativo: ${selectedObraOverride.titulo}` : 'Override de Obra'}
        footer={
          <>
            <Button variant="ghost" onClick={() => setSelectedObraOverride(null)}>Cancelar</Button>
            <Button variant="primary" onClick={handleSalvarOverride} isLoading={isOverriding}>Salvar Override</Button>
          </>
        }
      >
        <form onSubmit={handleSalvarOverride} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <Input
            label="Título da Obra"
            value={overrideTitulo}
            onChange={(e) => setOverrideTitulo(e.target.value)}
          />
          <Input
            label="Autor / Roteirista"
            value={overrideAutor}
            onChange={(e) => setOverrideAutor(e.target.value)}
          />
          <div>
            <label style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-secondary)', display: 'block', marginBottom: '6px' }}>
              Sinopse Curada
            </label>
            <textarea
              value={overrideSinopse}
              onChange={(e) => setOverrideSinopse(e.target.value)}
              rows={4}
              style={{
                width: '100%',
                background: 'var(--bg-glass-input)',
                border: '1px solid var(--border-subtle)',
                borderRadius: 'var(--radius-md)',
                padding: '9px 12px',
                color: 'var(--text-primary)',
                fontSize: '13px',
              }}
            />
          </div>
          <Input
            label="Justificativa do Override (Obrigatória para Auditoria)"
            placeholder="Ex: Correção oficial de metadados conforme editora nacional"
            value={overrideMotivo}
            onChange={(e) => setOverrideMotivo(e.target.value)}
            required
          />
        </form>
      </Modal>

      {/* Modal Gerenciamento de Gêneros */}
      <Modal
        isOpen={isGenreModalOpen}
        onClose={() => {
          setIsGenreModalOpen(false);
          handleCancelarEdicaoGenero();
        }}
        title="Gerenciar Gêneros Literários Canônicos"
        footer={
          <>
            <Button
              variant="ghost"
              onClick={() => {
                setIsGenreModalOpen(false);
                handleCancelarEdicaoGenero();
              }}
            >
              Fechar
            </Button>
            <Button variant="primary" onClick={handleCriarOuAtualizarGenero} isLoading={isSavingGenre}>
              {editingGenreId ? 'Salvar Alterações' : 'Cadastrar Gênero'}
            </Button>
          </>
        }
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {/* Formulário de Criação / Edição */}
          <form
            onSubmit={handleCriarOuAtualizarGenero}
            style={{
              display: 'flex',
              flexDirection: 'column',
              gap: '12px',
              padding: '16px',
              borderRadius: 'var(--radius-md)',
              background: 'var(--bg-card-hover)',
              border: '1px solid var(--border-subtle)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-primary)' }}>
                {editingGenreId ? '✏️ Editando Gênero Selecionado' : '➕ Novo Gênero Literário'}
              </span>
              {editingGenreId && (
                <button
                  type="button"
                  onClick={handleCancelarEdicaoGenero}
                  style={{
                    background: 'none',
                    border: 'none',
                    color: 'var(--text-muted)',
                    fontSize: '12px',
                    cursor: 'pointer',
                    textDecoration: 'underline',
                  }}
                >
                  Cancelar Edição
                </button>
              )}
            </div>
            <Input
              label="Nome do Gênero"
              placeholder="Ex: Cyberpunk, Ficção Científica"
              value={newGenreNome}
              onChange={(e) => setNewGenreNome(e.target.value)}
              required
            />
            <Input
              label="Descrição (Opcional)"
              placeholder="Breve definição ou escopo deste gênero"
              value={newGenreDescricao}
              onChange={(e) => setNewGenreDescricao(e.target.value)}
            />
          </form>

          {/* Lista de Gêneros Existentes */}
          <div>
            <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '8px' }}>
              Gêneros Cadastrados ({generos.length})
            </div>
            <div
              style={{
                maxHeight: '220px',
                overflowY: 'auto',
                display: 'flex',
                flexDirection: 'column',
                gap: '6px',
                paddingRight: '4px',
              }}
            >
              {generos.map((g) => (
                <div
                  key={g.id}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '8px 12px',
                    borderRadius: 'var(--radius-sm)',
                    background: editingGenreId === g.id ? 'rgba(99, 102, 241, 0.15)' : 'var(--bg-app)',
                    border: editingGenreId === g.id ? '1px solid var(--brand-primary)' : '1px solid var(--border-subtle)',
                  }}
                >
                  <div style={{ minWidth: 0, flex: 1 }}>
                    <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-primary)' }}>
                      {g.nome} <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>({g.slug})</span>
                    </div>
                    {g.descricao && (
                      <div
                        style={{
                          fontSize: '11px',
                          color: 'var(--text-secondary)',
                          whiteSpace: 'nowrap',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                        }}
                      >
                        {g.descricao}
                      </div>
                    )}
                  </div>
                  <div style={{ display: 'flex', gap: '6px', marginLeft: '12px' }}>
                    <button
                      type="button"
                      title="Editar gênero"
                      onClick={() => handleIniciarEdicaoGenero(g)}
                      style={{
                        padding: '4px 8px',
                        background: 'transparent',
                        border: '1px solid var(--border-subtle)',
                        borderRadius: 'var(--radius-sm)',
                        color: 'var(--text-primary)',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                      }}
                    >
                      <Edit2 size={13} />
                    </button>
                    <button
                      type="button"
                      title="Excluir gênero"
                      disabled={isDeletingGenreId === g.id}
                      onClick={() => handleExcluirGenero(g.id, g.nome)}
                      style={{
                        padding: '4px 8px',
                        background: 'rgba(239, 68, 68, 0.1)',
                        border: '1px solid rgba(239, 68, 68, 0.3)',
                        borderRadius: 'var(--radius-sm)',
                        color: '#EF4444',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                      }}
                    >
                      <Trash2 size={13} />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </Modal>

      {/* Modal Mesclar Tags */}
      <Modal
        isOpen={isMergeTagModalOpen}
        onClose={() => setIsMergeTagModalOpen(false)}
        title="Mesclar Tags Comunitárias Duplicadas"
        footer={
          <>
            <Button variant="ghost" onClick={() => setIsMergeTagModalOpen(false)}>Cancelar</Button>
            <Button variant="primary" onClick={handleMesclarTags} isLoading={isMergingTags}>Mesclar Tags</Button>
          </>
        }
      >
        <form onSubmit={handleMesclarTags} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
            Consolida votos e referências da tag de origem para a tag de destino.
          </p>
          <Input
            label="ID da Tag de Origem (A que será absorvida)"
            type="number"
            value={tagOrigemId}
            onChange={(e) => setTagOrigemId(e.target.value)}
            required
          />
          <Input
            label="ID da Tag de Destino (A que permanecerá ativa)"
            type="number"
            value={tagDestinoId}
            onChange={(e) => setTagDestinoId(e.target.value)}
            required
          />
          <Input
            label="Motivo da Mesclagem"
            placeholder="Ex: Padronização ortográfica / termo sinônimo"
            value={tagMotivo}
            onChange={(e) => setTagMotivo(e.target.value)}
            required
          />
        </form>
      </Modal>
    </div>
  );
};
