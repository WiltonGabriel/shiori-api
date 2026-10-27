package dev.shiori.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import dev.shiori.domain.enums.StatusSugestao;
import dev.shiori.domain.enums.TipoObra;
import dev.shiori.domain.model.Obra;
import dev.shiori.domain.model.Usuario;
import dev.shiori.infra.security.JwtTokenProvider;
import dev.shiori.repository.ObraRepository;
import dev.shiori.repository.SugestaoEdicaoObraRepository;
import dev.shiori.repository.UsuarioRepository;
import software.amazon.awssdk.services.s3.S3Client;

@SpringBootTest
@Transactional
@DisplayName("Testes de Integração: Ciclo de Vida e Governança de Sugestões de Edição de Obras")
class SugestaoEdicaoIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ObraRepository obraRepository;

    @Autowired
    private SugestaoEdicaoObraRepository sugestaoEdicaoObraRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private S3Client s3Client;

    private MockMvc mockMvc;
    private Usuario leitor;
    private Usuario admin;
    private String tokenLeitor;
    private String tokenAdmin;
    private Obra obra;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        leitor = new Usuario("leitor_contrib@shiori.dev", "hash_senha_123", "Leitor Contribuidor");
        leitor = usuarioRepository.save(leitor);
        tokenLeitor = jwtTokenProvider.gerarToken(leitor);

        admin = new Usuario("admin_curadoria@shiori.dev", "hash_senha_admin", "Admin Curador");
        admin.promoverPara(Role.ROLE_ADMIN);
        admin = usuarioRepository.save(admin);
        tokenAdmin = jwtTokenProvider.gerarToken(admin);

        obra = Obra.criarDeApiExterna("Obra Inicial", "Obra Initial", "MAL", "123456", 10, TipoObra.MANGA);
        obra = obraRepository.save(obra);
    }

    @Test
    @DisplayName("Deve submeter sugestão de edição com sucesso")
    void deveSubmeterSugestaoDeEdicaoComSucesso() throws Exception {
        String payload = """
            {
              "tituloPrincipal": "Obra Corrigida",
              "tituloOriginal": "Original Text",
              "sinopse": "Nova sinopse enriquecida",
              "totalCapitulosLancados": 50,
              "tipoObra": "MANHWA",
              "justificativa": "Atualização baseada nos dados oficiais da editora"
            }
            """;

        mockMvc.perform(post("/api/obras/" + obra.getId() + "/sugestoes-edicao")
                        .header("Authorization", "Bearer " + tokenLeitor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.obraId").value(obra.getId()))
                .andExpect(jsonPath("$.tituloPrincipalSugerido").value("Obra Corrigida"))
                .andExpect(jsonPath("$.totalCapitulosSugerido").value(50))
                .andExpect(jsonPath("$.tipoObraSugerido").value("MANHWA"))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.obraTitulo").value("Obra Inicial"));

        assertThat(sugestaoEdicaoObraRepository.findByObraIdAndStatus(obra.getId(), StatusSugestao.PENDENTE))
                .hasSize(1);
    }

    @Test
    @DisplayName("Admin deve listar sugestões com filtro de status")
    void adminDeveListarSugestoesComFiltroDeStatus() throws Exception {
        obra.sugerirEdicao(leitor, "Título Sugerido", null, null, null, null, null, "Correção");
        obraRepository.save(obra);

        mockMvc.perform(get("/api/admin/sugestoes")
                        .param("status", "PENDENTE")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].tituloPrincipalSugerido").value("Título Sugerido"))
                .andExpect(jsonPath("$.content[0].status").value("PENDENTE"));
    }

    @Test
    @DisplayName("Admin deve aprovar sugestão de edição e atualizar a obra no catálogo")
    void adminDeveAprovarSugestaoDeEdicaoEAtualizarObra() throws Exception {
        var sugestao = obra.sugerirEdicao(leitor, "Obra Atualizada Oficial", null, null, "Sinopse nova", 120, TipoObra.MANHWA, "Fonte confiável");
        sugestao = sugestaoEdicaoObraRepository.save(sugestao);

        String payloadAvaliar = """
            {
              "aprovado": true,
              "parecer": "Dados verificados e acurados."
            }
            """;

        mockMvc.perform(put("/api/admin/sugestoes/" + sugestao.getId() + "/avaliar")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadAvaliar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADA"))
                .andExpect(jsonPath("$.parecerAdmin").value("Dados verificados e acurados."));

        Obra obraRecarregada = obraRepository.findById(obra.getId()).orElseThrow();
        assertThat(obraRecarregada.getTituloPrincipal()).isEqualTo("Obra Atualizada Oficial");
        assertThat(obraRecarregada.getTotalCapitulosLancados()).isEqualTo(120);
        assertThat(obraRecarregada.getTipoObra()).isEqualTo(TipoObra.MANHWA);
    }
}
