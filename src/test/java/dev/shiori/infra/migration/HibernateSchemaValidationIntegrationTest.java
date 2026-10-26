package dev.shiori.infra.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.shiori.domain.model.AtividadeFeed;
import dev.shiori.domain.model.BroadcastSistema;
import dev.shiori.domain.model.Comentario;
import dev.shiori.domain.model.ConexaoUsuario;
import dev.shiori.domain.model.ConsentimentoUsuario;
import dev.shiori.domain.model.DenunciaComentario;
import dev.shiori.domain.model.DenunciaObra;
import dev.shiori.domain.model.FeatureFlag;
import dev.shiori.domain.model.Genero;
import dev.shiori.domain.model.HistoricoLeitura;
import dev.shiori.domain.model.ItemCatalogo;
import dev.shiori.domain.model.NoFederado;
import dev.shiori.domain.model.Obra;
import dev.shiori.domain.model.PareamentoInstancia;
import dev.shiori.domain.model.ProvedorIntegracao;
import dev.shiori.domain.model.RegistroAuditoria;
import dev.shiori.domain.model.SugestaoEdicaoObra;
import dev.shiori.domain.model.Tag;
import dev.shiori.domain.model.Usuario;
import dev.shiori.domain.model.VotoTag;

class HibernateSchemaValidationIntegrationTest {

    @Test
    @DisplayName("Deve validar conformidade estrita entre entidades JPA e o esquema migrado no PostgreSQL")
    void deveValidarConformidadeEntreEntidadesEEsquemaPostgres() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("hibernate.connection.url", "jdbc:postgresql://localhost:5432/shiori");
        settings.put("hibernate.connection.username", "shiori");
        settings.put("hibernate.connection.password", "shiori");
        settings.put("hibernate.connection.driver_class", "org.postgresql.Driver");
        settings.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        settings.put("hibernate.hbm2ddl.auto", "validate");
        settings.put("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySettings(settings)
                .build();

        try {
            MetadataSources sources = new MetadataSources(registry);
            sources.addAnnotatedClass(Usuario.class);
            sources.addAnnotatedClass(AtividadeFeed.class);
            sources.addAnnotatedClass(Obra.class);
            sources.addAnnotatedClass(ItemCatalogo.class);
            sources.addAnnotatedClass(Genero.class);
            sources.addAnnotatedClass(Tag.class);
            sources.addAnnotatedClass(HistoricoLeitura.class);
            sources.addAnnotatedClass(Comentario.class);
            sources.addAnnotatedClass(ConexaoUsuario.class);
            sources.addAnnotatedClass(ConsentimentoUsuario.class);
            sources.addAnnotatedClass(ProvedorIntegracao.class);
            sources.addAnnotatedClass(BroadcastSistema.class);
            sources.addAnnotatedClass(RegistroAuditoria.class);
            sources.addAnnotatedClass(SugestaoEdicaoObra.class);
            sources.addAnnotatedClass(DenunciaObra.class);
            sources.addAnnotatedClass(DenunciaComentario.class);
            sources.addAnnotatedClass(PareamentoInstancia.class);
            sources.addAnnotatedClass(NoFederado.class);
            sources.addAnnotatedClass(FeatureFlag.class);
            sources.addAnnotatedClass(VotoTag.class);

            var metadata = sources.buildMetadata();
            SchemaManagementToolCoordinator.process(
                    metadata,
                    registry,
                    settings,
                    null
            );

            assertThat(metadata.getEntityBindings()).hasSize(20);
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
