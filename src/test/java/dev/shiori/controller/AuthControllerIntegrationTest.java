package dev.shiori.controller;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import dev.shiori.domain.model.Usuario;
import dev.shiori.repository.UsuarioRepository;

@SpringBootTest
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/registrar deve cadastrar usuário e devolver token JWT")
    void deveRegistrarUsuarioComSucesso() throws Exception {
        String json = """
                {
                    "email": "novo.leitor@shiori.dev",
                    "senha": "senhaSegura123",
                    "apelido": "leitor_shiori"
                }
                """;

        mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.email").value("novo.leitor@shiori.dev"))
                .andExpect(jsonPath("$.apelido").value("leitor_shiori"))
                .andExpect(jsonPath("$.onboardingConcluido").value(true));
    }

    @Test
    @DisplayName("POST /api/auth/registrar deve rejeitar e-mail duplicado com ProblemDetail (RFC 9457)")
    void deveRejeitarRegistroComEmailDuplicado() throws Exception {
        usuarioRepository.save(new Usuario("existente@shiori.dev", "senhaHash123"));

        String json = """
                {
                    "email": "existente@shiori.dev",
                    "senha": "outraSenha123"
                }
                """;

        mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Regra de Negócio Violada"))
                .andExpect(jsonPath("$.detail").value("E-mail já cadastrado na plataforma"));
    }

    @Test
    @DisplayName("POST /api/auth/login deve autenticar com sucesso e devolver token")
    void deveAutenticarComSucesso() throws Exception {
        // Registra primeiro
        String jsonRegistro = """
                {
                    "email": "login.test@shiori.dev",
                    "senha": "senhaForte123",
                    "apelido": "user_login"
                }
                """;

        mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRegistro))
                .andExpect(status().isCreated());

        // Agora realiza o login
        String jsonLogin = """
                {
                    "email": "login.test@shiori.dev",
                    "senha": "senhaForte123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.email").value("login.test@shiori.dev"));
    }

    @Test
    @DisplayName("POST /api/auth/login com lembrarDeMim deve emitir token válido e persistente")
    void deveAutenticarComLembrarDeMim() throws Exception {
        String jsonRegistro = """
                {
                    "email": "remember.me@shiori.dev",
                    "senha": "senhaForte123",
                    "apelido": "user_remember"
                }
                """;

        mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRegistro))
                .andExpect(status().isCreated());

        String jsonLogin = """
                {
                    "email": "remember.me@shiori.dev",
                    "senha": "senhaForte123",
                    "lembrarDeMim": true
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.email").value("remember.me@shiori.dev"));
    }

    @Test
    @DisplayName("GET /api/auth/me sem token deve retornar 401 Unauthorized com ProblemDetail")
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Não Autorizado"));
    }

    @Test
    @DisplayName("GET /api/auth/me com token válido deve retornar 200 OK e dados do usuário")
    void deveRetornar200ComTokenValido() throws Exception {
        // Registra usuário
        String jsonRegistro = """
                {
                    "email": "perfil.test@shiori.dev",
                    "senha": "senhaForte123",
                    "apelido": "perfil_user"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRegistro))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String token = responseBody.split("\"token\":\"")[1].split("\"")[0];

        // Acessa rota protegida
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("perfil.test@shiori.dev"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.onboardingConcluido").value(true));
    }
}
