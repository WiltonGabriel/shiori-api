package dev.shiori.infra.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Teste de Integração Automatizado: Flyway Migration contra PostgreSQL Real.
 * Valida a execução em banco limpo, criação de esquema e idempotência.
 */
class FlywayPostgresMigrationIntegrationTest {

    private final String jdbcUrl = "jdbc:postgresql://localhost:5432/shiori";
    private final String username = "shiori";
    private final String password = "shiori";

    @Test
    @DisplayName("Deve executar migrações Flyway (V1 e V2) com sucesso em banco PostgreSQL e validar histórico")
    void deveExecutarMigracaoV1ComSucessoEmPostgres() throws Exception {
        // 1. Limpar schema public antes do teste
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public;");
        }

        // 2. Configurar Flyway
        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, username, password)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load();

        // 3. Executar migrações
        MigrateResult result = flyway.migrate();
        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).isGreaterThanOrEqualTo(2);

        // 4. Verificar tabela de histórico do Flyway (V1 e V2)
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT version, description, success FROM flyway_schema_history WHERE version = '1'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("description")).isEqualTo("schema inicial shiori");
            assertThat(rs.getBoolean("success")).isTrue();
        }

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT version, description, success FROM flyway_schema_history WHERE version = '2'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("description")).isEqualTo("governanca sugestoes tags");
            assertThat(rs.getBoolean("success")).isTrue();
        }

        // 5. Verificar que tabelas essenciais existem
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {
            ResultSet rsUsers = stmt.executeQuery("SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'tb_usuario'");
            assertThat(rsUsers.next()).isTrue();
            assertThat(rsUsers.getInt(1)).isEqualTo(1);

            ResultSet rsObras = stmt.executeQuery("SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'tb_obra'");
            assertThat(rsObras.next()).isTrue();
            assertThat(rsObras.getInt(1)).isEqualTo(1);

            ResultSet rsSugestoes = stmt.executeQuery("SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'tb_sugestao_tag_obra'");
            assertThat(rsSugestoes.next()).isTrue();
            assertThat(rsSugestoes.getInt(1)).isEqualTo(1);
        }

        // 6. Idempotência: rodar novamente não deve falhar nem reaplicar
        MigrateResult secondRun = flyway.migrate();
        assertThat(secondRun.success).isTrue();
        assertThat(secondRun.migrationsExecuted).isEqualTo(0);
    }
}
