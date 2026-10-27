package dev.shiori.service.admin;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.shiori.domain.enums.StatusSugestao;
import dev.shiori.domain.exception.RecursoNaoEncontradoException;
import dev.shiori.domain.exception.RegraDeNegocioException;
import dev.shiori.domain.model.Genero;
import dev.shiori.domain.model.ItemCatalogo;
import dev.shiori.domain.model.Obra;
import dev.shiori.domain.model.SugestaoEdicaoObra;
import dev.shiori.domain.model.Tag;
import dev.shiori.domain.model.Usuario;
import dev.shiori.domain.model.VotoTag;
import dev.shiori.dto.ObraResponse;
import dev.shiori.dto.admin.AvaliarSugestaoRequest;
import dev.shiori.dto.admin.CriarGeneroRequest;
import dev.shiori.dto.admin.GeneroResponse;
import dev.shiori.dto.admin.MesclarObrasRequest;
import dev.shiori.dto.admin.MesclarObrasResponse;
import dev.shiori.dto.admin.MesclarTagsRequest;
import dev.shiori.dto.admin.MesclarTagsResponse;
import dev.shiori.dto.admin.OverrideObraRequest;
import dev.shiori.dto.admin.SugestaoEdicaoResponse;
import dev.shiori.repository.AtividadeFeedRepository;
import dev.shiori.repository.ComentarioRepository;
import dev.shiori.repository.GeneroRepository;
import dev.shiori.repository.ItemCatalogoRepository;
import dev.shiori.repository.ObraRepository;
import dev.shiori.repository.SugestaoEdicaoObraRepository;
import dev.shiori.repository.TagRepository;
import dev.shiori.repository.VotoTagRepository;

/**
 * Serviço responsável exclusivamente pela Curadoria do Catálogo e Metadados Literários:
 * - Mesclagem de obras duplicadas e preservação de ciclos e histórico de leitura.
 * - Oficialização de obras comunitárias e overrides administrativos blindados contra sync.
 * - Cadastro de novos gêneros e unificação/mesclagem de tags canônicas.
 * - Avaliação de sugestões de edição comunitárias.
 * Adere estritamente ao Princípio da Responsabilidade Única (SRP) e Clean Code.
 */
@Service
@Transactional
public class AdminCuradoriaService {

    private final AdminAuditoriaService auditoriaService;
    private final ObraRepository obraRepository;
    private final ItemCatalogoRepository itemCatalogoRepository;
    private final ComentarioRepository comentarioRepository;
    private final SugestaoEdicaoObraRepository sugestaoEdicaoObraRepository;
    private final AtividadeFeedRepository atividadeFeedRepository;
    private final GeneroRepository generoRepository;
    private final TagRepository tagRepository;
    private final VotoTagRepository votoTagRepository;

    public AdminCuradoriaService(AdminAuditoriaService auditoriaService,
                                 ObraRepository obraRepository,
                                 ItemCatalogoRepository itemCatalogoRepository,
                                 ComentarioRepository comentarioRepository,
                                 SugestaoEdicaoObraRepository sugestaoEdicaoObraRepository,
                                 AtividadeFeedRepository atividadeFeedRepository,
                                 GeneroRepository generoRepository,
                                 TagRepository tagRepository,
                                 VotoTagRepository votoTagRepository) {
        this.auditoriaService = auditoriaService;
        this.obraRepository = obraRepository;
        this.itemCatalogoRepository = itemCatalogoRepository;
        this.comentarioRepository = comentarioRepository;
        this.sugestaoEdicaoObraRepository = sugestaoEdicaoObraRepository;
        this.atividadeFeedRepository = atividadeFeedRepository;
        this.generoRepository = generoRepository;
        this.tagRepository = tagRepository;
        this.votoTagRepository = votoTagRepository;
    }

    public MesclarObrasResponse mesclarObras(MesclarObrasRequest request, Long adminId) {
        Usuario admin = auditoriaService.obterEValidarAdmin(adminId);
        Long obraDuplicadaId = request.obraDuplicadaId();
        Long obraPrincipalId = request.obraPrincipalId();

        if (obraDuplicadaId.equals(obraPrincipalId)) {
            throw new RegraDeNegocioException("A obra duplicada e a obra principal não podem ser a mesma obra");
        }

        Obra duplicada = obraRepository.findById(obraDuplicadaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Obra duplicada não encontrada: ID " + obraDuplicadaId));
        Obra principal = obraRepository.findById(obraPrincipalId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Obra principal não encontrada: ID " + obraPrincipalId));

        principal.incorporarMetadadosDe(duplicada);

        List<ItemCatalogo> itensDuplicados = itemCatalogoRepository.findByObraId(obraDuplicadaId);
        int itensTransferidos = 0;
        int itensIncorporados = 0;

        for (ItemCatalogo itemDuplicado : itensDuplicados) {
            Optional<ItemCatalogo> itemPrincipalExistente = itemCatalogoRepository
                    .findByUsuarioIdAndObraId(itemDuplicado.getUsuario().getId(), obraPrincipalId);

            if (itemPrincipalExistente.isPresent()) {
                itemPrincipalExistente.get().incorporarCiclosDe(itemDuplicado);
                itemCatalogoRepository.delete(itemDuplicado);
                itensIncorporados++;
            } else {
                itemDuplicado.transferirParaObra(principal);
                itemCatalogoRepository.save(itemDuplicado);
                itensTransferidos++;
            }
        }
        itemCatalogoRepository.flush();

        int comentariosTransferidos = comentarioRepository.transferirObra(obraDuplicadaId, obraPrincipalId);
        sugestaoEdicaoObraRepository.transferirObra(obraDuplicadaId, obraPrincipalId);
        atividadeFeedRepository.transferirObra(obraDuplicadaId, obraPrincipalId);

        obraRepository.delete(duplicada);
        Obra obraSalva = obraRepository.save(principal);

        String detalhes = "Mesclada duplicada [%d: '%s'] na principal [%d: '%s']. Itens transferidos: %d, incorporados: %d, comentários: %d"
                .formatted(obraDuplicadaId, duplicada.getTituloPrincipal(), obraPrincipalId, obraSalva.getTituloPrincipal(),
                        itensTransferidos, itensIncorporados, comentariosTransferidos);

        auditoriaService.registrarAuditoria(admin, "MESCLAR_OBRAS", "Obra", String.valueOf(obraPrincipalId), detalhes);

        return new MesclarObrasResponse(
                obraSalva.getId(),
                obraSalva.getTituloPrincipal(),
                obraDuplicadaId,
                itensTransferidos,
                itensIncorporados,
                comentariosTransferidos,
                "Obras mescladas com sucesso sem perda de dados."
        );
    }

    public ObraResponse oficializarObra(Long obraId, Long adminId) {
        Usuario admin = auditoriaService.obterEValidarAdmin(adminId);
        Obra obra = obraRepository.findById(obraId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Obra não encontrada: ID " + obraId));

        obra.oficializarPorAdmin(admin);
        Obra obraSalva = obraRepository.save(obra);

        auditoriaService.registrarAuditoria(admin, "OFICIALIZAR_OBRA", "Obra", String.valueOf(obraId),
                "Obra promovida a status OFICIAL: " + obraSalva.getTituloPrincipal());

        return ObraResponse.fromEntity(obraSalva);
    }

    public ObraResponse aplicarOverrideObra(Long obraId, OverrideObraRequest request, Long adminId) {
        Usuario admin = auditoriaService.obterEValidarAdmin(adminId);
        Obra obra = obraRepository.findById(obraId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Obra não encontrada: ID " + obraId));

        obra.aplicarOverrideAdministrativo(
                admin,
                request.tituloPrincipal(),
                request.tituloOriginal(),
                request.capaUrl(),
                request.sinopse(),
                request.totalCapitulosLancados(),
                request.totalVolumes()
        );

        Obra obraSalva = obraRepository.save(obra);
        auditoriaService.registrarAuditoria(admin, "OVERRIDE_OBRA", "Obra", String.valueOf(obraId),
                "Override aplicado e obra protegida contra sync automático");

        return ObraResponse.fromEntity(obraSalva);
    }

    public GeneroResponse criarGenero(CriarGeneroRequest request, Long adminId) {
        Usuario admin = auditoriaService.obterEValidarAdmin(adminId);
        String nomeNormalizado = request.nome().trim();
        if (generoRepository.existsByNomeIgnoreCase(nomeNormalizado)) {
            throw new RegraDeNegocioException("Gênero com esse nome já cadastrado: " + nomeNormalizado);
        }

        Genero genero = new Genero(nomeNormalizado, request.descricao());
        Genero generoSalvo = generoRepository.save(genero);

        auditoriaService.registrarAuditoria(admin, "CRIAR_GENERO", "Genero", String.valueOf(generoSalvo.getId()),
                "Gênero cadastrado: " + generoSalvo.getNome());

        return GeneroResponse.deEntidade(generoSalvo);
    }

    @Transactional(readOnly = true)
    public List<GeneroResponse> listarGeneros(Long adminId) {
        auditoriaService.obterEValidarAdmin(adminId);
        return generoRepository.findAll().stream()
                .map(GeneroResponse::deEntidade)
                .toList();
    }

    public MesclarTagsResponse mesclarTags(MesclarTagsRequest request, Long adminId) {
        Usuario admin = auditoriaService.obterEValidarAdmin(adminId);

        Tag tagDestino = tagRepository.findById(request.tagDestinoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tag de destino não encontrada: ID " + request.tagDestinoId()));

        Set<Long> origensUnicas = new HashSet<>(request.tagOrigemIds());
        origensUnicas.remove(tagDestino.getId());

        int tagsMescladas = 0;
        int obrasAtualizadas = 0;

        for (Long origemId : origensUnicas) {
            Optional<Tag> tagOrigemOpt = tagRepository.findById(origemId);
            if (tagOrigemOpt.isEmpty()) {
                continue;
            }
            Tag tagOrigem = tagOrigemOpt.get();

            List<Obra> obrasComTag = obraRepository.findByTagsContains(tagOrigem);
            for (Obra obra : obrasComTag) {
                obra.removerTag(tagOrigem);
                obra.adicionarTag(tagDestino);
                obraRepository.save(obra);
                obrasAtualizadas++;
            }

            List<VotoTag> votos = votoTagRepository.findByTagId(tagOrigem.getId());
            for (VotoTag voto : votos) {
                Optional<VotoTag> votoExistente = votoTagRepository
                        .findByUsuarioIdAndTagIdAndObraId(voto.getUsuario().getId(), tagDestino.getId(), voto.getObra().getId());
                if (votoExistente.isPresent()) {
                    votoTagRepository.delete(voto);
                }
            }
            votoTagRepository.flush();
            votoTagRepository.deletarPorTagId(tagOrigem.getId());

            tagRepository.delete(tagOrigem);
            tagsMescladas++;
        }

        String detalhes = "Mescladas %d tags na tag canônica [%d: '%s']. Obras afetadas: %d"
                .formatted(tagsMescladas, tagDestino.getId(), tagDestino.getNome(), obrasAtualizadas);

        auditoriaService.registrarAuditoria(admin, "MESCLAR_TAGS", "Tag", String.valueOf(tagDestino.getId()), detalhes);

        return new MesclarTagsResponse(
                tagDestino.getId(),
                tagDestino.getNome(),
                tagsMescladas,
                obrasAtualizadas,
                "Tags mescladas com sucesso na tag oficial."
        );
    }

    @Transactional(readOnly = true)
    public Page<SugestaoEdicaoResponse> listarSugestoesPendentes(Pageable pageable, Long adminId) {
        return listarSugestoes(StatusSugestao.PENDENTE, pageable, adminId);
    }

    @Transactional(readOnly = true)
    public Page<SugestaoEdicaoResponse> listarSugestoes(StatusSugestao status, Pageable pageable, Long adminId) {
        auditoriaService.obterEValidarAdmin(adminId);
        if (status != null) {
            return sugestaoEdicaoObraRepository.findByStatus(status, pageable)
                    .map(SugestaoEdicaoResponse::deEntidade);
        }
        return sugestaoEdicaoObraRepository.findAll(pageable)
                .map(SugestaoEdicaoResponse::deEntidade);
    }

    public SugestaoEdicaoResponse avaliarSugestao(Long sugestaoId, AvaliarSugestaoRequest request, Long adminId) {
        Usuario admin = auditoriaService.obterEValidarAdmin(adminId);
        SugestaoEdicaoObra sugestao = sugestaoEdicaoObraRepository.findById(sugestaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sugestão de edição não encontrada: ID " + sugestaoId));

        if (Boolean.TRUE.equals(request.aprovado())) {
            sugestao.aprovar(admin, request.parecer());
        } else {
            sugestao.rejeitar(admin, request.parecer());
        }

        SugestaoEdicaoObra salva = sugestaoEdicaoObraRepository.save(sugestao);
        auditoriaService.registrarAuditoria(admin, "AVALIAR_SUGESTAO", "SugestaoEdicaoObra", String.valueOf(salva.getId()),
                "Avaliação da sugestão: status=" + salva.getStatus() + ", parecer: " + request.parecer());

        return SugestaoEdicaoResponse.deEntidade(salva);
    }
}
