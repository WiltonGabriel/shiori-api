package dev.shiori.infra.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import software.amazon.awssdk.services.s3.S3Client;

@SpringBootTest
@DisplayName("Testes de Integração: AdminSpaWebConfig e Entregas Estáticas /admin")
class AdminSpaWebIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private S3Client s3Client;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Deve redirecionar /admin para /admin/")
    void deveRedirecionarAdminSemBarraParaAdminComBarra() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/"));
    }

    @Test
    @DisplayName("Deve entregar HTML da SPA em /admin/")
    void deveEntregarHtmlSpaEmAdminComBarra() throws Exception {
        mockMvc.perform(get("/admin/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"root\"")));
    }

    @Test
    @DisplayName("Deve entregar index.html para rotas de navegação cliente SPA (/admin/integracoes)")
    void deveEntregarIndexHtmlParaRotasNavegacaoSpa() throws Exception {
        mockMvc.perform(get("/admin/integracoes"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"root\"")));

        mockMvc.perform(get("/admin/usuarios"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"root\"")));

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"root\"")));
    }

    @Test
    @DisplayName("Deve entregar arquivos estáticos de ativos (/admin/assets/...)")
    void deveEntregarArquivosEstaticosAssets() throws Exception {
        String cssName = "index-DI9nQTua.css";
        String jsName = "index-Bvr02mVI.js";

        java.io.File assetsDir = new java.io.File("src/main/resources/static/admin/assets");
        if (assetsDir.exists() && assetsDir.isDirectory()) {
            java.io.File[] files = assetsDir.listFiles();
            if (files != null) {
                for (java.io.File f : files) {
                    if (f.getName().endsWith(".css")) {
                        cssName = f.getName();
                    } else if (f.getName().endsWith(".js")) {
                        jsName = f.getName();
                    }
                }
            }
        }

        mockMvc.perform(get("/admin/assets/" + cssName))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/css"));

        mockMvc.perform(get("/admin/assets/" + jsName))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve manter endpoints da API REST (/api/admin/**) protegidos com 401 quando desautenticado")
    void deveManterEndpointsApiAdminProtegidos() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/integracoes"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/tags/sugestoes"))
                .andExpect(status().isUnauthorized());
    }
}
