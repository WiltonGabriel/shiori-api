package dev.shiori.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import dev.shiori.domain.enums.Role;
import dev.shiori.domain.enums.TipoObra;
import dev.shiori.domain.model.Obra;
import dev.shiori.domain.model.Tag;
import dev.shiori.domain.model.Usuario;
import dev.shiori.infra.security.JwtTokenProvider;
import dev.shiori.repository.ObraRepository;
import dev.shiori.repository.RegistroAuditoriaRepository;
import dev.shiori.repository.SugestaoTagObraRepository;
import dev.shiori.repository.TagRepository;
import dev.shiori.repository.UsuarioRepository;
import dev.shiori.repository.VotoTagRepository;
import software.amazon.awssdk.services.s3.S3Client;

@SpringBootTest
@Transactional
@DisplayName("Testes de Integração: Governança, Moderação e Ciclo de Vida de Tags Comunitárias")
class TagGovernanceIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ObraRepository obraRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private SugestaoTagObraRepository sugestaoTagObraRepository;

    @Autowired
    private VotoTagRepository votoTagRepository;

    @Autowired
    private RegistroAuditoriaRepository registroAuditoriaRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private S3Client s3Client;

    private MockMvc mockMvc;

    private Usuario usuarioLeitor;
    private String tokenLeitor;

    private Usuario usuarioOutro;
    private String tokenOutro;

    private Usuario usuarioCurador;
    private String tokenCurador;

    private Usuario usuarioAdmin;
    private String tokenAdmin;

    private Obra obraFrieren;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // 1. Cria leitores comuns
        usuarioLeitor = new Usuario("leitor1@shiori.dev", "senhaForte123", "leitor_frieren");
        usuarioLeitor = usuarioRepository.save(usuarioLeitor);
        tokenLeitor = jwtTokenProvider.gerarToken(usuarioLeitor);

        usuarioOutro = new Usuario("leitor2@shiori.dev", "senhaForte123", "leitor_aventura");
        usuarioOutro = usuarioRepository.save(usuarioOutro);
        tokenOutro = jwtTokenProvider.gerarToken(usuarioOutro);

        // 2. Cria curador e admin
        usuarioCurador = new Usuario("curador@shiori.dev", "senhaForte123", "curador_oficial");
        usuarioCurador.promoverPara(Role.ROLE_CURADOR);
        usuarioCurador = usuarioRepository.save(usuarioCurador);
        tokenCurador = jwtTokenProvider.gerarToken(usuarioCurador);

        usuarioAdmin = new Usuario("admin.tag@shiori.dev", "senhaForte123", "admin_tag");
        usuarioAdmin.promoverPara(Role.ROLE_ADMIN);
        usuarioAdmin = usuarioRepository.save(usuarioAdmin);
        tokenAdmin = jwtTokenProvider.gerarToken(usuarioAdmin);

        // 3. Cria obra oficial para testes de tags
        obraFrieren = Obra.criarDeApiExterna("Sousou no Frieren", "Frieren: Beyond Journey's End", "MAL", "12345", 100, TipoObra.MANGA);
        obraFrieren = obraRepository.save(obraFrieren);
    }

    @Test
    @DisplayName("Sugerir tag sem autenticação deve retornar 401 Unauthorized")
    void sugerirTagSemAutenticacaoDeveRetornar401() throws Exception {
        String json = """
                {
                    "nomeTag": "Viagem Melancólica",
                    "justificativa": "A atmosfera da obra é contemplativa."
                }
                """;

        mockMvc.perform(post("/api/obras/{id}/tags/sugestoes", obraFrieren.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Usuário autenticado sugere tag com sucesso; sugestão fica PENDENTE e não polui tags da obra")
    void sugerirTagComSucessoFicaPendenteENaoApareceEmTagsOficiais() throws Exception {
        String json = """
                {
                    "nomeTag": "Jornada Épica",
                    "justificativa": "História acompanha a jornada de anos."
                }
                """;

        mockMvc.perform(post("/api/obras/{id}/tags/sugestoes", obraFrieren.getId())
                        .header("Authorization", "Bearer " + tokenLeitor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomeTag").value("Jornada Épica"))
                .andExpect(jsonPath("$.slugTag").value("jornada-epica"))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.usuarioSugeridorApelido").value("leitor_frieren"));

        // A tag pendente NÃO deve aparecer na listagem pública de tags da obra
        mockMvc.perform(get("/api/obras/{id}/tags", obraFrieren.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Não deve permitir sugerir a mesma tag pendente duas vezes para a mesma obra")
    void naoDevePermitirDuplicarSugestaoPendente() throws Exception {
        String json = """
                {
                    "nomeTag": "Magia Ancestral",
                    "justificativa": "Primeira sugestão"
                }
                """;

        mockMvc.perform(post("/api/obras/{id}/tags/sugestoes", obraFrieren.getId())
                        .header("Authorization", "Bearer " + tokenLeitor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        // Segunda tentativa com nome equivalente (mesmo em maiúsculas)
        String jsonDuplicado = """
                {
                    "nomeTag": "MAGIA ANCESTRAL",
                    "justificativa": "Tentativa duplicada"
                }
                """;

        mockMvc.perform(post("/api/obras/{id}/tags/sugestoes", obraFrieren.getId())
                        .header("Authorization", "Bearer " + tokenOutro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonDuplicado))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Já existe uma sugestão pendente")));
    }

    @Test
    @DisplayName("Curador aprova sugestão: normaliza tag, associa à obra, audita e concede upvote ao autor")
    void curadorAprovaSugestaoComSucesso() throws Exception {
        // 1. Leitor submete sugestão
        String jsonSugestao = """
                {
                    "nomeTag": "Elfos e Longevidade",
                    "justificativa": "Tema central da protagonista"
                }
                """;

        String responseBody = mockMvc.perform(post("/api/obras/{id}/tags/sugestoes", obraFrieren.getId())
                        .header("Authorization", "Bearer " + tokenLeitor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSugestao))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long sugestaoId = com.jayway.jsonpath.JsonPath.parse(responseBody).read("$.id", Long.class);

        // 2. Usuário comum tenta aprovar -> 403 Forbidden
        mockMvc.perform(post("/api/admin/tags/sugestoes/{id}/aprovar", sugestaoId)
                        .header("Authorization", "Bearer " + tokenLeitor))
                .andExpect(status().isForbidden());

        // 3. Curador aprova a sugestão -> 200 OK
        mockMvc.perform(post("/api/admin/tags/sugestoes/{id}/aprovar", sugestaoId)
                        .header("Authorization", "Bearer " + tokenCurador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADA"))
                .andExpect(jsonPath("$.adminAvaliadorApelido").value("curador_oficial"));

        // 4. Verifica se a tag agora aparece na obra com score 1
        mockMvc.perform(get("/api/obras/{id}/tags", obraFrieren.getId())
                        .header("Authorization", "Bearer " + tokenLeitor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("Elfos e Longevidade"))
                .andExpect(jsonPath("$[0].slug").value("elfos-e-longevidade"))
                .andExpect(jsonPath("$[0].score").value(1))
                .andExpect(jsonPath("$[0].meuVoto").value("UPVOTE"));

        // 5. Verifica registro de auditoria gravado
        boolean auditoGravado = registroAuditoriaRepository.findAll().stream()
                .anyMatch(reg -> "APROVAR_SUGESTAO_TAG".equals(reg.getAcao()));
        assertThat(auditoGravado).isTrue();
    }

    @Test
    @DisplayName("Admin rejeita sugestão com motivo: não associa tag e permite nova sugestão posterior")
    void adminRejeitaSugestaoComMotivo() throws Exception {
        // 1. Leitor submete sugestão de tag inadequada
        String jsonSugestao = """
                {
                    "nomeTag": "Tag Inadequada",
                    "justificativa": "Sem justificativa relevante"
                }
                """;

        String responseBody = mockMvc.perform(post("/api/obras/{id}/tags/sugestoes", obraFrieren.getId())
                        .header("Authorization", "Bearer " + tokenLeitor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSugestao))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long sugestaoId = com.jayway.jsonpath.JsonPath.parse(responseBody).read("$.id", Long.class);

        // 2. Rejeição sem motivo obrigatório deve falhar com 400
        mockMvc.perform(post("/api/admin/tags/sugestoes/{id}/rejeitar", sugestaoId)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"\"}"))
                .andExpect(status().isBadRequest());

        // 3. Admin rejeita com motivo válido
        mockMvc.perform(post("/api/admin/tags/sugestoes/{id}/rejeitar", sugestaoId)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"Tag redundante ou fora dos padrões da comunidade.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJEITADA"))
                .andExpect(jsonPath("$.motivoRejeicao").value("Tag redundante ou fora dos padrões da comunidade."))
                .andExpect(jsonPath("$.adminAvaliadorApelido").value("admin_tag"));

        // 4. Obra não possui essa tag
        mockMvc.perform(get("/api/obras/{id}/tags", obraFrieren.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 5. O usuário tem liberdade de submeter uma nova sugestão após a rejeição da anterior
        mockMvc.perform(post("/api/obras/{id}/tags/sugestoes", obraFrieren.getId())
                        .header("Authorization", "Bearer " + tokenLeitor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "nomeTag": "Tag Inadequada",
                                    "justificativa": "Nova tentativa com argumentos melhores."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    @DisplayName("Votação comunitária: único voto por usuário/tag/obra, alternância UPVOTE/DOWNVOTE e score negativo oculta tag")
    void votacaoComunitariaCicloCompleto() throws Exception {
        // 1. Cria e aprova uma tag oficial na obra
        String jsonSugestao = """
                {
                    "nomeTag": "Spoiler Enredo",
                    "justificativa": "Contém elementos cruciais."
                }
                """;

        String responseBody = mockMvc.perform(post("/api/obras/{id}/tags/sugestoes", obraFrieren.getId())
                        .header("Authorization", "Bearer " + tokenLeitor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSugestao))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long sugestaoId = com.jayway.jsonpath.JsonPath.parse(responseBody).read("$.id", Long.class);

        mockMvc.perform(post("/api/admin/tags/sugestoes/{id}/aprovar", sugestaoId)
                        .header("Authorization", "Bearer " + tokenCurador))
                .andExpect(status().isOk());

        // Obtém o ID da tag aprovada
        String tagsResponse = mockMvc.perform(get("/api/obras/{id}/tags", obraFrieren.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long tagId = com.jayway.jsonpath.JsonPath.parse(tagsResponse).read("$[0].tagId", Long.class);

        // 2. Outro usuário vota DOWNVOTE na tag (score passa de 1 para 0)
        mockMvc.perform(post("/api/obras/{id}/tags/{tagId}/votar", obraFrieren.getId(), tagId)
                        .header("Authorization", "Bearer " + tokenOutro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoVoto\": \"DOWNVOTE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreAtual").value(0))
                .andExpect(jsonPath("$.meuVoto").value("DOWNVOTE"));

        // 3. O autor da tag muda de UPVOTE para DOWNVOTE -> score cai para -2
        mockMvc.perform(post("/api/obras/{id}/tags/{tagId}/votar", obraFrieren.getId(), tagId)
                        .header("Authorization", "Bearer " + tokenLeitor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoVoto\": \"DOWNVOTE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreAtual").value(-2))
                .andExpect(jsonPath("$.meuVoto").value("DOWNVOTE"));

        // 4. Como score < 0, a tag fica oculta da listagem pública comunitária
        mockMvc.perform(get("/api/obras/{id}/tags", obraFrieren.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 5. Garantir que o número de registros de voto não duplicou por usuário (1 voto por usuário na tag)
        long votosDoOutro = votoTagRepository.findAll().stream()
                .filter(v -> v.getUsuario().getId().equals(usuarioOutro.getId()) && v.getTag().getId().equals(tagId))
                .count();
        assertThat(votosDoOutro).isEqualTo(1);
    }

    @Test
    @DisplayName("Autocomplete/pesquisa de tags existentes permite reutilização sem duplicar no vocabulário")
    void pesquisaTagsPermiteReuso() throws Exception {
        // Tag pré-existente
        Tag tagExistente = new Tag("Slice of Life", usuarioAdmin.getId());
        tagRepository.save(tagExistente);

        mockMvc.perform(get("/api/tags").param("termo", "Slice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Slice of Life"))
                .andExpect(jsonPath("$[0].slug").value("slice-of-life"));
    }
}
