package dev.shiori.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class PostgresContainerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Deve conectar com sucesso ao PostgreSQL 16 via Testcontainers e validar versão nativa")
    void deveConectarAoPostgres16Nativo() {
        assertThat(postgres.isRunning()).isTrue();

        String versao = jdbcTemplate.queryForObject("SELECT version()", String.class);
        assertThat(versao).containsIgnoringCase("PostgreSQL 16");
    }
}
