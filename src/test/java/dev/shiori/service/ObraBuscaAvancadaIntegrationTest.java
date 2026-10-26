package dev.shiori.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import dev.shiori.domain.enums.ModoFiltroColecao;
import dev.shiori.domain.enums.StatusCatalogo;
import dev.shiori.domain.enums.StatusPublicacao;
import dev.shiori.domain.enums.TipoObra;
import dev.shiori.domain.model.Genero;
import dev.shiori.domain.model.Obra;
import dev.shiori.domain.model.Tag;
import dev.shiori.dto.ObraResponse;
import dev.shiori.dto.obra.ObraBuscaAvancadaFiltro;
import dev.shiori.dto.obra.OrdenacaoObra;
import dev.shiori.repository.GeneroRepository;
import dev.shiori.repository.ObraRepository;
import dev.shiori.repository.TagRepository;

@SpringBootTest
@Transactional
@DisplayName("Testes de Integração: Busca Avançada Paginada do Catálogo (Series Finder)")
class ObraBuscaAvancadaIntegrationTest {

    @Autowired
    private ObraService obraService;

    @Autowired
    private ObraRepository obraRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private Obra soloLeveling;
    private Obra omniscientReader;
    private Obra mushokuTensei;
    private Obra horimiya;
    private Obra kaguyaSama;

    private Genero generoAcao;
    private Genero generoFantasia;
    private Genero generoMisterio;
    private Genero generoRomance;
    private Genero generoComedia;

    private Tag tagDungeon;
    private Tag tagProtagonistaOp;
    private Tag tagIsekai;
    private Tag tagVidaEscolar;
    private Tag tagGuerraMental;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        // Limpar dados prévios
        obraRepository.deleteAll();

        // 1. Criar Gêneros
        generoAcao = generoRepository.findByNomeIgnoreCase("Ação")
                .orElseGet(() -> generoRepository.save(new Genero("Ação", "Narrativas de combate e ação")));
        generoFantasia = generoRepository.findByNomeIgnoreCase("Fantasia")
                .orElseGet(() -> generoRepository.save(new Genero("Fantasia", "Mundos mágicos")));
        generoMisterio = generoRepository.findByNomeIgnoreCase("Mistério")
                .orElseGet(() -> generoRepository.save(new Genero("Mistério", "Enigmas e segredos")));
        generoRomance = generoRepository.findByNomeIgnoreCase("Romance")
                .orElseGet(() -> generoRepository.save(new Genero("Romance", "Relações amorosas")));
        generoComedia = generoRepository.findByNomeIgnoreCase("Comédia")
                .orElseGet(() -> generoRepository.save(new Genero("Comédia", "Humor e sátira")));

        // 2. Criar Tags
        tagDungeon = tagRepository.findByNomeIgnoreCase("Dungeon")
                .orElseGet(() -> tagRepository.save(new Tag("Dungeon", 1L)));
        tagProtagonistaOp = tagRepository.findByNomeIgnoreCase("Protagonista OP")
                .orElseGet(() -> tagRepository.save(new Tag("Protagonista OP", 1L)));
        tagIsekai = tagRepository.findByNomeIgnoreCase("Isekai")
                .orElseGet(() -> tagRepository.save(new Tag("Isekai", 1L)));
        tagVidaEscolar = tagRepository.findByNomeIgnoreCase("Vida Escolar")
                .orElseGet(() -> tagRepository.save(new Tag("Vida Escolar", 1L)));
        tagGuerraMental = tagRepository.findByNomeIgnoreCase("Guerra Mental")
                .orElseGet(() -> tagRepository.save(new Tag("Guerra Mental", 1L)));

        // 3. Criar Obras Oficiais
        soloLeveling = Obra.criarDeApiExterna("Solo Leveling", "Na Honjaman Rebeleob", null, "Caçador mais fraco se torna o mais forte.", 179, TipoObra.MANHWA);
        soloLeveling.atualizarNotaGeral(9.2);
        soloLeveling.adicionarGenero(generoAcao);
        soloLeveling.adicionarGenero(generoFantasia);
        soloLeveling.adicionarTag(tagDungeon);
        soloLeveling.adicionarTag(tagProtagonistaOp);
        soloLeveling = obraRepository.save(soloLeveling);

        omniscientReader = Obra.criarDeApiExterna("Omniscient Reader's Viewpoint", "Jeonjijeok Dokja Sijeom", null, "O mundo da novel se torna realidade.", 220, TipoObra.MANHWA);
        omniscientReader.atualizarNotaGeral(9.5);
        omniscientReader.adicionarGenero(generoAcao);
        omniscientReader.adicionarGenero(generoFantasia);
        omniscientReader.adicionarGenero(generoMisterio);
        omniscientReader.adicionarTag(tagProtagonistaOp);
        omniscientReader = obraRepository.save(omniscientReader);

        mushokuTensei = Obra.criarDeApiExterna("Mushoku Tensei", "Isekai Ittara Honki Dasu", null, "Reencarnação no mundo de fantasia mágica.", 26, TipoObra.LIGHT_NOVEL);
        mushokuTensei.atualizarNotaGeral(8.8);
        mushokuTensei.adicionarGenero(generoFantasia);
        mushokuTensei.adicionarTag(tagIsekai);
        mushokuTensei = obraRepository.save(mushokuTensei);

        horimiya = Obra.criarDeApiExterna("Horimiya", "Hori-san to Miyamura-kun", null, "Dois jovens com lados secretos se conhecem.", 125, TipoObra.MANGA);
        horimiya.atualizarNotaGeral(8.5);
        horimiya.adicionarGenero(generoRomance);
        horimiya.adicionarGenero(generoComedia);
        horimiya.adicionarTag(tagVidaEscolar);
        horimiya = obraRepository.save(horimiya);

        kaguyaSama = Obra.criarDeApiExterna("Kaguya-sama: Love Is War", "Kaguya-sama wa Kokurasetai", null, "Guerra de intelectos entre gênios apaixonados.", 281, TipoObra.MANGA);
        kaguyaSama.atualizarNotaGeral(9.0);
        kaguyaSama.adicionarGenero(generoRomance);
        kaguyaSama.adicionarGenero(generoComedia);
        kaguyaSama.adicionarTag(tagVidaEscolar);
        kaguyaSama.adicionarTag(tagGuerraMental);
        kaguyaSama = obraRepository.save(kaguyaSama);
    }

    @Test
    @DisplayName("Busca por termo deve localizar no título principal e título original")
    void deveBuscarPorTermoNoTituloPrincipalEOriginal() {
        ObraBuscaAvancadaFiltro filtroPrincipal = new ObraBuscaAvancadaFiltro(
                "solo", null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, 0, 10
        );
        Page<ObraResponse> res1 = obraService.buscarObrasAvancada(filtroPrincipal);
        assertThat(res1.getContent()).extracting(ObraResponse::tituloPrincipal)
                .containsExactly("Solo Leveling");

        ObraBuscaAvancadaFiltro filtroOriginal = new ObraBuscaAvancadaFiltro(
                "Dokja", null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, 0, 10
        );
        Page<ObraResponse> res2 = obraService.buscarObrasAvancada(filtroOriginal);
        assertThat(res2.getContent()).extracting(ObraResponse::tituloPrincipal)
                .containsExactly("Omniscient Reader's Viewpoint");
    }

    @Test
    @DisplayName("Filtro por múltiplos tipos literários (MANHWA e LIGHT_NOVEL)")
    void deveFiltrarPorMultiplosTipos() {
        ObraBuscaAvancadaFiltro filtro = new ObraBuscaAvancadaFiltro(
                null, Set.of(TipoObra.MANHWA, TipoObra.LIGHT_NOVEL), null, null, null, null, null, null,
                null, null, null, null, null, null, OrdenacaoObra.TITULO_ASC, 0, 10
        );
        Page<ObraResponse> res = obraService.buscarObrasAvancada(filtro);
        assertThat(res.getContent()).extracting(ObraResponse::tituloPrincipal)
                .containsExactly("Mushoku Tensei", "Omniscient Reader's Viewpoint", "Solo Leveling");
    }

    @Test
    @DisplayName("Filtro por intervalo numérico de capítulos (200 a 300)")
    void deveFiltrarPorIntervaloDeCapitulos() {
        ObraBuscaAvancadaFiltro filtro = new ObraBuscaAvancadaFiltro(
                null, null, null, null, 200, 300, null, null,
                null, null, null, null, null, null, OrdenacaoObra.CAPITULOS_DESC, 0, 10
        );
        Page<ObraResponse> res = obraService.buscarObrasAvancada(filtro);
        assertThat(res.getContent()).extracting(ObraResponse::tituloPrincipal)
                .containsExactly("Kaguya-sama: Love Is War", "Omniscient Reader's Viewpoint");
    }

    @Test
    @DisplayName("Filtro por intervalo numérico de notas (9.0 a 9.3)")
    void deveFiltrarPorIntervaloDeNotas() {
        ObraBuscaAvancadaFiltro filtro = new ObraBuscaAvancadaFiltro(
                null, null, null, null, null, null, 9.0, 9.3,
                null, null, null, null, null, null, OrdenacaoObra.NOTA_DESC, 0, 10
        );
        Page<ObraResponse> res = obraService.buscarObrasAvancada(filtro);
        assertThat(res.getContent()).extracting(ObraResponse::tituloPrincipal)
                .containsExactly("Solo Leveling", "Kaguya-sama: Love Is War");
    }

    @Test
    @DisplayName("Gêneros com AND: exige que a obra contenha TODOS os gêneros solicitados")
    void deveFiltrarGenerosComModoAnd() {
        // Exige Ação E Fantasia (Solo Leveling e Omniscient Reader têm ambos; Mushoku Tensei tem Fantasia mas não Ação)
        ObraBuscaAvancadaFiltro filtro = new ObraBuscaAvancadaFiltro(
                null, null, null, null, null, null, null, null,
                Set.of("Ação", "Fantasia"), ModoFiltroColecao.AND, null, null, null, null,
                OrdenacaoObra.NOTA_DESC, 0, 10
        );
        Page<ObraResponse> res = obraService.buscarObrasAvancada(filtro);
        assertThat(res.getContent()).extracting(ObraResponse::tituloPrincipal)
                .containsExactly("Omniscient Reader's Viewpoint", "Solo Leveling");
    }

    @Test
    @DisplayName("Gêneros com OR: retorna obras que contenham PELO MENOS UM dos gêneros")
    void deveFiltrarGenerosComModoOr() {
        // Exige Mistério OU Romance
        ObraBuscaAvancadaFiltro filtro = new ObraBuscaAvancadaFiltro(
                null, null, null, null, null, null, null, null,
                Set.of("Mistério", "Romance"), ModoFiltroColecao.OR, null, null, null, null,
                OrdenacaoObra.NOTA_DESC, 0, 10
        );
        Page<ObraResponse> res = obraService.buscarObrasAvancada(filtro);
        assertThat(res.getContent()).extracting(ObraResponse::tituloPrincipal)
                .containsExactly("Omniscient Reader's Viewpoint", "Kaguya-sama: Love Is War", "Horimiya");
    }

    @Test
    @DisplayName("Gêneros com NOT: exclui qualquer obra que possua o gênero proibido")
    void deveExcluirObrasComGenerosProibidos() {
        // Busca Fantasia, mas EXCLUI Mistério (elimina Omniscient Reader, mantém Solo Leveling e Mushoku Tensei)
        ObraBuscaAvancadaFiltro filtro = new ObraBuscaAvancadaFiltro(
                null, null, null, null, null, null, null, null,
                Set.of("Fantasia"), ModoFiltroColecao.AND, Set.of("Mistério"), null, null, null,
                OrdenacaoObra.NOTA_DESC, 0, 10
        );
        Page<ObraResponse> res = obraService.buscarObrasAvancada(filtro);
        assertThat(res.getContent()).extracting(ObraResponse::tituloPrincipal)
                .containsExactly("Solo Leveling", "Mushoku Tensei");
    }

    @Test
    @DisplayName("Tags com NOT: exclui obras que contenham a tag proibida")
    void deveExcluirObrasComTagsProibidas() {
        // Busca com Tag "Vida Escolar", mas EXCLUI "Guerra Mental" (mantém Horimiya, elimina Kaguya-sama)
        ObraBuscaAvancadaFiltro filtro = new ObraBuscaAvancadaFiltro(
                null, null, null, null, null, null, null, null,
                null, null, null, Set.of("Vida Escolar"), ModoFiltroColecao.AND, Set.of("Guerra Mental"),
                OrdenacaoObra.NOTA_DESC, 0, 10
        );
        Page<ObraResponse> res = obraService.buscarObrasAvancada(filtro);
        assertThat(res.getContent()).extracting(ObraResponse::tituloPrincipal)
                .containsExactly("Horimiya");
    }

    @Test
    @DisplayName("Garantia de não duplicidade: obras com múltiplos relacionamentos não devem se repetir na paginação")
    void naoDeveDuplicarObrasNaPaginacao() {
        // Busca ampla por todas as obras com Fantasia ou Romance
        ObraBuscaAvancadaFiltro filtro = new ObraBuscaAvancadaFiltro(
                null, null, null, null, null, null, null, null,
                Set.of("Fantasia", "Romance"), ModoFiltroColecao.OR, null, null, null, null,
                OrdenacaoObra.NOTA_DESC, 0, 20
        );
        Page<ObraResponse> res = obraService.buscarObrasAvancada(filtro);

        long idsDistintos = res.getContent().stream().map(ObraResponse::id).distinct().count();
        assertThat(res.getContent()).hasSize((int) idsDistintos);
    }

    @Test
    @DisplayName("Paginação e ordenação estável entre páginas consecutivas")
    void devePaginarComOrdenacaoEstavel() {
        // Ordenação por nota descrescente: Omniscient (9.5), Solo (9.2), Kaguya (9.0), Mushoku (8.8), Horimiya (8.5)
        ObraBuscaAvancadaFiltro pagina0 = new ObraBuscaAvancadaFiltro(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, OrdenacaoObra.NOTA_DESC, 0, 2
        );
        Page<ObraResponse> res0 = obraService.buscarObrasAvancada(pagina0);
        assertThat(res0.getContent()).extracting(ObraResponse::tituloPrincipal)
                .containsExactly("Omniscient Reader's Viewpoint", "Solo Leveling");
        assertThat(res0.getTotalElements()).isEqualTo(5);
        assertThat(res0.getTotalPages()).isEqualTo(3);

        ObraBuscaAvancadaFiltro pagina1 = new ObraBuscaAvancadaFiltro(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, OrdenacaoObra.NOTA_DESC, 1, 2
        );
        Page<ObraResponse> res1 = obraService.buscarObrasAvancada(pagina1);
        assertThat(res1.getContent()).extracting(ObraResponse::tituloPrincipal)
                .containsExactly("Kaguya-sama: Love Is War", "Mushoku Tensei");
    }

    @Test
    @DisplayName("Resultado vazio quando nenhum registro coincide com os critérios")
    void deveRetornarPaginaVaziaSemErros() {
        ObraBuscaAvancadaFiltro filtroIncompativel = new ObraBuscaAvancadaFiltro(
                "TermoInexistenteXYZ", null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, 0, 10
        );
        Page<ObraResponse> res = obraService.buscarObrasAvancada(filtroIncompativel);
        assertThat(res.getContent()).isEmpty();
        assertThat(res.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Endpoint HTTP GET /api/obras/busca-avancada via MockMvc")
    void deveExecutarEndpointHttpMockMvc() throws Exception {
        mockMvc.perform(get("/api/obras/busca-avancada")
                        .param("tipos", "MANHWA")
                        .param("minCapitulos", "150")
                        .param("ordenacao", "NOTA_DESC")
                        .param("pagina", "0")
                        .param("itensPorPagina", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].tituloPrincipal").value("Omniscient Reader's Viewpoint"))
                .andExpect(jsonPath("$.content[1].tituloPrincipal").value("Solo Leveling"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("Preservação de retrocompatibilidade do endpoint legado GET /obras")
    void devePreservarCompatibilidadeDoEndpointLegado() throws Exception {
        mockMvc.perform(get("/obras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(5));
    }
}
