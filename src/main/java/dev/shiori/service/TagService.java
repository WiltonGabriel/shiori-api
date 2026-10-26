package dev.shiori.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.shiori.domain.enums.StatusSugestaoTag;
import dev.shiori.domain.enums.TipoVoto;
import dev.shiori.domain.exception.RecursoNaoEncontradoException;
import dev.shiori.domain.exception.RegraDeNegocioException;
import dev.shiori.domain.model.Obra;
import dev.shiori.domain.model.SugestaoTagObra;
import dev.shiori.domain.model.Tag;
import dev.shiori.domain.model.Usuario;
import dev.shiori.domain.model.VotoTag;
import dev.shiori.dto.tag.SugestaoTagAdminResponse;
import dev.shiori.dto.tag.SugestaoTagResponse;
import dev.shiori.dto.tag.SugerirTagRequest;
import dev.shiori.dto.tag.TagObraResponse;
import dev.shiori.dto.tag.TagResponse;
import dev.shiori.dto.tag.VotoTagResponse;
import dev.shiori.repository.ObraRepository;
import dev.shiori.repository.SugestaoTagObraRepository;
import dev.shiori.repository.TagRepository;
import dev.shiori.repository.UsuarioRepository;
import dev.shiori.repository.VotoTagRepository;
import dev.shiori.service.admin.AdminAuditoriaService;

/**
 * Serviço de regras de negócio para a governança comunitária de tags,
 * submissão de sugestões, moderação administrativa e votação única ponderada (SRP / Clean Code).
 */
@Service
@Transactional
public class TagService {

    private static final Logger log = LoggerFactory.getLogger(TagService.class);
    private static final int MAX_SUGESTOES_PENDENTES_POR_USUARIO = 10;

    private final SugestaoTagObraRepository sugestaoTagObraRepository;
    private final TagRepository tagRepository;
    private final ObraRepository obraRepository;
    private final UsuarioRepository usuarioRepository;
    private final VotoTagRepository votoTagRepository;
    private final AdminAuditoriaService adminAuditoriaService;

    public TagService(
            SugestaoTagObraRepository sugestaoTagObraRepository,
            TagRepository tagRepository,
            ObraRepository obraRepository,
            UsuarioRepository usuarioRepository,
            VotoTagRepository votoTagRepository,
            AdminAuditoriaService adminAuditoriaService) {
        this.sugestaoTagObraRepository = sugestaoTagObraRepository;
        this.tagRepository = tagRepository;
        this.obraRepository = obraRepository;
        this.usuarioRepository = usuarioRepository;
        this.votoTagRepository = votoTagRepository;
        this.adminAuditoriaService = adminAuditoriaService;
    }

    /**
     * Permite que um usuário autenticado sugira vincular uma tag existente ou um novo termo a uma obra.
     */
    @Transactional
    public SugestaoTagResponse sugerirTagParaObra(Long obraId, Long usuarioId, SugerirTagRequest request) {
        if (obraId == null) {
            throw new RegraDeNegocioException("O identificador da obra é obrigatório");
        }
        if (usuarioId == null) {
            throw new RegraDeNegocioException("Usuário não autenticado");
        }
        if (request == null) {
            throw new RegraDeNegocioException("Os dados da sugestão são obrigatórios");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: ID " + usuarioId));

        if (!usuario.isAtivo()) {
            throw new RegraDeNegocioException("Usuários inativos não podem enviar sugestões");
        }

        Obra obra = obraRepository.findById(obraId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Obra não encontrada: ID " + obraId));

        // 1. Limite anti-spam de sugestões pendentes por usuário
        long pendentesDoUsuario = sugestaoTagObraRepository.countByUsuarioSugeridorIdAndStatus(usuarioId, StatusSugestaoTag.PENDENTE);
        if (pendentesDoUsuario >= MAX_SUGESTOES_PENDENTES_POR_USUARIO) {
            throw new RegraDeNegocioException(
                    "Você atingiu o limite de sugestões pendentes simultâneas (máximo de " + MAX_SUGESTOES_PENDENTES_POR_USUARIO + "). Aguarde a avaliação dos moderadores."
            );
        }

        // 2. Resolução do termo e busca defensiva no catálogo de tags
        Tag tagExistente = null;
        String nomeTag;

        if (request.tagId() != null) {
            tagExistente = tagRepository.findById(request.tagId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Tag não encontrada: ID " + request.tagId()));
            nomeTag = tagExistente.getNome();
        } else if (request.nomeTag() != null && !request.nomeTag().trim().isBlank()) {
            nomeTag = request.nomeTag().trim();
            if (nomeTag.length() > 60) {
                throw new RegraDeNegocioException("O nome da tag não pode exceder 60 caracteres");
            }
            // Tenta reaproveitar tag já existente no banco por nome ou slug
            Optional<Tag> tagPorNome = tagRepository.findByNomeIgnoreCase(nomeTag);
            if (tagPorNome.isPresent()) {
                tagExistente = tagPorNome.get();
            }
        } else {
            throw new RegraDeNegocioException("Informe o identificador de uma tag existente ou o nome do novo termo");
        }

        // 3. Validação: A tag já está associada à obra?
        String nomeNormalizado = tagExistente != null ? tagExistente.getNome() : nomeTag;
        String slugNormalizado = tagExistente != null ? tagExistente.getSlug() : null;

        boolean jaAssociada = obra.getTags().stream().anyMatch(t ->
                t.getNome().equalsIgnoreCase(nomeNormalizado) ||
                        (slugNormalizado != null && t.getSlug().equalsIgnoreCase(slugNormalizado))
        );
        if (jaAssociada) {
            throw new RegraDeNegocioException("Esta tag já está associada a esta obra");
        }

        // 4. Validação: Já existe sugestão PENDENTE para esta tag nesta obra?
        boolean existePendente = sugestaoTagObraRepository.existsByObraIdAndNomeTagIgnoreCaseAndStatus(
                obraId, nomeNormalizado, StatusSugestaoTag.PENDENTE
        );
        if (!existePendente && tagExistente != null && tagExistente.getId() != null) {
            existePendente = sugestaoTagObraRepository.existsByObraIdAndTagIdAndStatus(
                    obraId, tagExistente.getId(), StatusSugestaoTag.PENDENTE
            );
        }
        if (existePendente) {
            throw new RegraDeNegocioException("Já existe uma sugestão pendente para esta tag nesta obra");
        }

        // 5. Persistir sugestão em estado PENDENTE (nunca entra em tb_obra_tag automaticamente)
        SugestaoTagObra sugestao = new SugestaoTagObra(obra, tagExistente, nomeNormalizado, usuario, request.justificativa());
        SugestaoTagObra salva = sugestaoTagObraRepository.save(sugestao);

        log.info("SUGESTAO_TAG: Usuário [id={}] sugeriu a tag '{}' para a obra [id={}] com status PENDENTE",
                usuario.getId(), salva.getNomeTag(), obra.getId());

        return SugestaoTagResponse.fromEntity(salva);
    }

    /**
     * Retorna a lista de tags oficiais associadas à obra, filtrando scores negativos e enriquecendo com voto do usuário.
     */
    @Transactional(readOnly = true)
    public List<TagObraResponse> listarTagsDaObra(Long obraId, Long usuarioLogadoId) {
        Obra obra = obraRepository.findById(obraId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Obra não encontrada: ID " + obraId));

        List<TagObraResponse> responses = new ArrayList<>();

        for (Tag tag : obra.getTags()) {
            int score = obra.calcularScoreTag(tag);
            // Regra de Domínio: Tags com score negativo são ocultadas do público
            if (score < 0) {
                continue;
            }

            TipoVoto meuVoto = null;
            if (usuarioLogadoId != null) {
                meuVoto = votoTagRepository
                        .findByUsuarioIdAndTagIdAndObraId(usuarioLogadoId, tag.getId(), obraId)
                        .map(VotoTag::getTipoVoto)
                        .orElse(null);
            }

            responses.add(new TagObraResponse(tag.getId(), tag.getNome(), tag.getSlug(), score, meuVoto));
        }

        // Ordenar por maior relevância (score decrescente) e alfabeticamente
        responses.sort(Comparator.comparingInt(TagObraResponse::score).reversed()
                .thenComparing(TagObraResponse::nome));

        return responses;
    }

    /**
     * Registra ou atualiza um voto de relevância da comunidade (UPVOTE ou DOWNVOTE) em uma tag oficial da obra.
     */
    @Transactional
    public VotoTagResponse votarEmTag(Long obraId, Long tagId, Long usuarioId, TipoVoto tipoVoto) {
        if (obraId == null || tagId == null) {
            throw new RegraDeNegocioException("Identificadores da obra e da tag são obrigatórios");
        }
        if (usuarioId == null) {
            throw new RegraDeNegocioException("Usuário não autenticado");
        }
        if (tipoVoto == null) {
            throw new RegraDeNegocioException("O tipo de voto (UPVOTE ou DOWNVOTE) é obrigatório");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: ID " + usuarioId));

        if (!usuario.isAtivo()) {
            throw new RegraDeNegocioException("Usuários inativos não podem votar");
        }

        Obra obra = obraRepository.findById(obraId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Obra não encontrada: ID " + obraId));

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tag não encontrada: ID " + tagId));

        if (!obra.getTags().contains(tag)) {
            throw new RegraDeNegocioException("A tag informada não está associada a esta obra");
        }

        // 1. Delegar alteração ou inserção à entidade Obra (garante unicidade uk_voto_usuario_tag_obra)
        obra.computarVotoTag(tag, usuario, tipoVoto);
        obraRepository.save(obra);

        // 2. Persistir especificamente no VotoTagRepository caso JPA cascade não tenha sincronizado
        Optional<VotoTag> votoExistente = votoTagRepository.findByUsuarioIdAndTagIdAndObraId(usuarioId, tagId, obraId);
        if (votoExistente.isPresent()) {
            votoExistente.get().alterarVoto(tipoVoto);
            votoTagRepository.save(votoExistente.get());
        } else {
            votoTagRepository.save(new VotoTag(usuario, tag, obra, tipoVoto));
        }

        int scoreAtualizado = obra.calcularScoreTag(tag);
        return new VotoTagResponse(tag.getId(), obra.getId(), scoreAtualizado, tipoVoto);
    }

    /**
     * Lista as sugestões de tags para avaliação por administradores ou curadores.
     */
    @Transactional(readOnly = true)
    public Page<SugestaoTagAdminResponse> listarSugestoes(StatusSugestaoTag status, Pageable pageable, Long operadorId) {
        adminAuditoriaService.obterEValidarCuradorOuAdmin(operadorId);

        StatusSugestaoTag statusEfetivo = status != null ? status : StatusSugestaoTag.PENDENTE;
        Page<SugestaoTagObra> pagina = sugestaoTagObraRepository.findByStatusOrderByDataSugestaoDesc(statusEfetivo, pageable);
        return pagina.map(SugestaoTagAdminResponse::fromEntity);
    }

    /**
     * Aprova uma sugestão de tag de maneira transacional e idempotente, vinculando a tag à obra.
     */
    @Transactional
    public SugestaoTagAdminResponse aprovarSugestao(Long sugestaoId, Long operadorId) {
        Usuario operador = adminAuditoriaService.obterEValidarCuradorOuAdmin(operadorId);

        SugestaoTagObra sugestao = sugestaoTagObraRepository.findById(sugestaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sugestão de tag não encontrada: ID " + sugestaoId));

        // Idempotência
        if (sugestao.getStatus() == StatusSugestaoTag.APROVADA) {
            return SugestaoTagAdminResponse.fromEntity(sugestao);
        }

        if (sugestao.getStatus() != StatusSugestaoTag.PENDENTE) {
            throw new RegraDeNegocioException("Apenas sugestões pendentes podem ser aprovadas");
        }

        // 1. Resolver ou criar a entrada definitiva na taxonomia de Tags
        Tag tagDefinitiva = sugestao.getTag();
        if (tagDefinitiva == null) {
            String slugEsperado = sugestao.gerarSlug();
            tagDefinitiva = tagRepository.findByNomeIgnoreCase(sugestao.getNomeTag())
                    .or(() -> tagRepository.findBySlugIgnoreCase(slugEsperado))
                    .orElseGet(() -> tagRepository.save(new Tag(sugestao.getNomeTag(), operador.getId())));
        }

        // 2. Vincular à Obra
        Obra obra = obraRepository.findById(sugestao.getObra().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Obra não encontrada: ID " + sugestao.getObra().getId()));

        if (!obra.getTags().contains(tagDefinitiva)) {
            obra.adicionarTag(tagDefinitiva);
        }

        // 3. Atualizar estado da sugestão
        sugestao.aprovar(operador, tagDefinitiva);
        SugestaoTagObra salva = sugestaoTagObraRepository.save(sugestao);

        // 4. Bonificar o usuário autor da sugestão com um UPVOTE automático
        if (votoTagRepository.findByUsuarioIdAndTagIdAndObraId(sugestao.getUsuarioSugeridor().getId(), tagDefinitiva.getId(), obra.getId()).isEmpty()) {
            obra.computarVotoTag(tagDefinitiva, sugestao.getUsuarioSugeridor(), TipoVoto.UPVOTE);
        }
        obraRepository.save(obra);

        // 5. Auditoria de Segurança
        adminAuditoriaService.registrarAuditoria(
                operador,
                "APROVAR_SUGESTAO_TAG",
                "SugestaoTagObra",
                String.valueOf(salva.getId()),
                String.format("Tag '%s' aprovada e vinculada à obra '%s'", tagDefinitiva.getNome(), obra.getTituloPrincipal())
        );

        return SugestaoTagAdminResponse.fromEntity(salva);
    }

    /**
     * Rejeita uma sugestão pendente com justificativa formal.
     */
    @Transactional
    public SugestaoTagAdminResponse rejeitarSugestao(Long sugestaoId, Long operadorId, String motivo) {
        Usuario operador = adminAuditoriaService.obterEValidarCuradorOuAdmin(operadorId);

        SugestaoTagObra sugestao = sugestaoTagObraRepository.findById(sugestaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sugestão de tag não encontrada: ID " + sugestaoId));

        sugestao.rejeitar(operador, motivo);
        SugestaoTagObra salva = sugestaoTagObraRepository.save(sugestao);

        adminAuditoriaService.registrarAuditoria(
                operador,
                "REJEITAR_SUGESTAO_TAG",
                "SugestaoTagObra",
                String.valueOf(salva.getId()),
                String.format("Sugestão da tag '%s' rejeitada para a obra '%s'. Motivo: %s",
                        salva.getNomeTag(), salva.getObra().getTituloPrincipal(), motivo)
        );

        return SugestaoTagAdminResponse.fromEntity(salva);
    }

    /**
     * Pesquisa textual de tags cadastradas no catálogo global.
     */
    @Transactional(readOnly = true)
    public List<TagResponse> pesquisarTags(String termo, Integer limite) {
        int max = (limite != null && limite > 0) ? Math.min(limite, 50) : 20;
        Pageable pageable = PageRequest.of(0, max);

        if (termo == null || termo.trim().isBlank()) {
            return tagRepository.findAll(pageable).map(TagResponse::fromEntity).getContent();
        }

        return tagRepository.findByNomeContainingIgnoreCase(termo.trim(), pageable)
                .map(TagResponse::fromEntity)
                .getContent();
    }
}
