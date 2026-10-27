package dev.shiori.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.shiori.domain.enums.StatusCatalogo;
import dev.shiori.domain.enums.TipoObra;
import dev.shiori.domain.exception.RecursoNaoEncontradoException;
import dev.shiori.domain.model.Obra;
import dev.shiori.domain.model.Usuario;
import dev.shiori.dto.ObraCadastroRequest;
import dev.shiori.dto.ObraResponse;
import dev.shiori.dto.ResultadoBuscaObrasResponse;
import dev.shiori.dto.obra.ObraBuscaAvancadaFiltro;
import dev.shiori.repository.AtividadeFeedRepository;
import dev.shiori.repository.ItemCatalogoRepository;
import dev.shiori.repository.ObraRepository;
import dev.shiori.repository.UsuarioRepository;
import dev.shiori.repository.specification.ObraSpecification;

@Service
@Transactional(readOnly = true)
public class ObraService {

    private final ObraRepository obraRepository;
    private final UsuarioRepository usuarioRepository;
    private final ItemCatalogoRepository itemCatalogoRepository;
    private final AtividadeFeedRepository atividadeFeedRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public ObraService(
            ObraRepository obraRepository,
            UsuarioRepository usuarioRepository,
            ItemCatalogoRepository itemCatalogoRepository,
            AtividadeFeedRepository atividadeFeedRepository) {
        this.obraRepository = obraRepository;
        this.usuarioRepository = usuarioRepository;
        this.itemCatalogoRepository = itemCatalogoRepository;
        this.atividadeFeedRepository = atividadeFeedRepository;
    }

    public ObraService(ObraRepository obraRepository, UsuarioRepository usuarioRepository) {
        this(obraRepository, usuarioRepository, null, null);
    }

    @Transactional
    public ObraResponse cadastrarObra(ObraCadastroRequest dados) {
        Obra obra = new Obra(
                dados.tituloPrincipal(),
                dados.tituloOriginal(),
                dados.capaUrl(),
                dados.sinopse(),
                dados.totalCapitulosLancados(),
                dados.tipoObra()
        );

        Obra obraSalva = obraRepository.save(obra);
        return ObraResponse.fromEntity(obraSalva);
    }

    @Transactional
    public ObraResponse submeterAoCatalogoGlobal(Long obraId, Long usuarioId) {
        Obra obra = buscarObraPorId(obraId);
        obra.submeterAoCatalogoGlobal(usuarioId);
        Obra obraSalva = obraRepository.save(obra);
        return ObraResponse.fromEntity(obraSalva);
    }

    @Transactional
    public ObraResponse promoverParaOficial(Long obraId) {
        Obra obra = buscarObraPorId(obraId);
        obra.promoverParaOficial();
        Obra obraSalva = obraRepository.save(obra);
        return ObraResponse.fromEntity(obraSalva);
    }

    public List<ObraResponse> listarTodas(StatusCatalogo statusFiltro) {
        return listarTodas(statusFiltro, null);
    }

    public List<ObraResponse> listarTodas(StatusCatalogo statusFiltro, Long usuarioId) {
        List<Obra> obras;

        if (statusFiltro == null) {
            obras = obraRepository.findAll();
        } else {
            obras = obraRepository.findByStatusCatalogo(statusFiltro);
        }

        Set<TipoObra> formatosPermitidos = obterFormatosPermitidos(usuarioId);
        if (formatosPermitidos != null && !formatosPermitidos.isEmpty()) {
            obras = obras.stream()
                    .filter(o -> formatosPermitidos.contains(o.getTipoObra()))
                    .toList();
        }

        return obras.stream()
                .map(ObraResponse::fromEntity)
                .toList();
    }

    public List<ObraResponse> listarObrasDaHome() {
        return listarObrasDaHome(null);
    }

    public List<ObraResponse> listarObrasDaHome(Long usuarioId) {
        List<Obra> obras = obraRepository.findByStatusCatalogoIn(
                List.of(StatusCatalogo.OFICIAL, StatusCatalogo.COMUNITARIO)
        );

        Set<TipoObra> formatosPermitidos = obterFormatosPermitidos(usuarioId);
        if (formatosPermitidos != null && !formatosPermitidos.isEmpty()) {
            obras = obras.stream()
                    .filter(o -> formatosPermitidos.contains(o.getTipoObra()))
                    .toList();
        }

        return obras.stream()
                .map(ObraResponse::fromEntity)
                .toList();
    }

    public ObraResponse buscarPorId(Long obraId) {
        Obra obra = buscarObraPorId(obraId);
        return ObraResponse.fromEntity(obra);
    }

    /**
     * Busca assistida inteligente:
     * - Se houver correspondências dentro dos formatos preferidos do usuário, retorna as obras normalmente.
     * - Se NÃO houver correspondências nos formatos permitidos, mas existirem em outros formatos (desativados):
     *   oculta os cards/capas e retorna um aviso contextual indicando os formatos e orientando para as Configurações.
     */
    public ResultadoBuscaObrasResponse buscarObrasComAssistencia(String termo, Long usuarioId) {
        if (termo == null || termo.trim().isBlank()) {
            return ResultadoBuscaObrasResponse.vazio();
        }

        List<Obra> todasCorrespondencias = obraRepository.pesquisarPorTermo(termo.trim());
        if (todasCorrespondencias.isEmpty()) {
            return ResultadoBuscaObrasResponse.vazio();
        }

        Set<TipoObra> permitidos = obterFormatosPermitidos(usuarioId);
        if (permitidos == null || permitidos.isEmpty()) {
            return ResultadoBuscaObrasResponse.comResultados(
                    todasCorrespondencias.stream().map(ObraResponse::fromEntity).toList()
            );
        }

        List<Obra> obrasPermitidas = todasCorrespondencias.stream()
                .filter(o -> permitidos.contains(o.getTipoObra()))
                .toList();

        if (!obrasPermitidas.isEmpty()) {
            return ResultadoBuscaObrasResponse.comResultados(
                    obrasPermitidas.stream().map(ObraResponse::fromEntity).toList()
            );
        }

        // Não há resultados nos formatos permitidos, mas foram encontrados em outros formatos
        Set<TipoObra> formatosDetectados = todasCorrespondencias.stream()
                .map(o -> o.getTipoObra())
                .collect(Collectors.toSet());

        String nomesFormatos = formatosDetectados.stream()
                .map(t -> t.getDescricao())
                .collect(Collectors.joining(", "));

        String mensagem = String.format(
                "Identificamos obras correspondentes a '%s' disponíveis no(s) formato(s) %s, porém eles estão desativados em suas preferências de leitura. Ajuste suas preferências em Configurações caso deseje visualizá-las.",
                termo.trim(),
                nomesFormatos
        );

        return ResultadoBuscaObrasResponse.avisoOutrosFormatos(formatosDetectados, mensagem);
    }

    /**
     * Executa a busca avançada paginada no banco de dados via JPA Specification / Criteria API.
     * Suporta filtros combinados de texto, formatos, status, faixas numéricas de capítulos/notas
     * e relacionamentos de gêneros e tags com lógica booleana AND, OR e NOT.
     */
    @Transactional(readOnly = true)
    public Page<ObraResponse> buscarObrasAvancada(ObraBuscaAvancadaFiltro filtro) {
        ObraBuscaAvancadaFiltro filtroEfetivo = filtro != null
                ? filtro
                : new ObraBuscaAvancadaFiltro(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        filtroEfetivo.validar();

        Sort sort = ObraSpecification.construirSort(filtroEfetivo.ordenacao());
        Pageable pageable = PageRequest.of(filtroEfetivo.pagina(), filtroEfetivo.itensPorPagina(), sort);
        Specification<Obra> spec = ObraSpecification.construir(filtroEfetivo);

        Page<Obra> paginaObras = obraRepository.findAll(spec, pageable);
        return paginaObras.map(ObraResponse::fromEntity);
    }

    @Transactional
    public void excluirObra(Long obraId) {
        if (!obraRepository.existsById(obraId)) {
            throw new RecursoNaoEncontradoException("Obra não encontrada com ID: " + obraId);
        }
        // Soberania da estante: Desvincula itens da estante e atividades sociais sem apagar dados locais do leitor
        if (itemCatalogoRepository != null) {
            itemCatalogoRepository.desvincularObra(obraId);
        }
        if (atividadeFeedRepository != null) {
            atividadeFeedRepository.desvincularObra(obraId);
        }
        obraRepository.deleteById(obraId);
    }

    private Obra buscarObraPorId(Long id) {
        return obraRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Obra não encontrada com ID: " + id));
    }

    private Set<TipoObra> obterFormatosPermitidos(Long usuarioId) {
        if (usuarioId == null) {
            return null;
        }
        return usuarioRepository.findById(usuarioId)
                .map(u -> u.getFormatosPreferidos())
                .orElse(null);
    }
}
