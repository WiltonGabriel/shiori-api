package dev.shiori.controller;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
@DisplayName("Testes de Segurança: GeneroController Endpoints Públicos")
class GeneroControllerSecurityTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("GET /generos deve ser público (retornar 200 OK sem autenticação)")
    void getGenerosDeveSerPublico() throws Exception {
        mockMvc.perform(get("/generos"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/generos deve ser público (retornar 200 OK sem autenticação)")
    void getApiGenerosDeveSerPublico() throws Exception {
        mockMvc.perform(get("/api/generos"))
                .andExpect(status().isOk());
    }
}
