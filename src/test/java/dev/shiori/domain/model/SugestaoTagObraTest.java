package dev.shiori.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.shiori.domain.enums.Role;
import dev.shiori.domain.enums.StatusSugestaoTag;
import dev.shiori.domain.enums.TipoObra;
import dev.shiori.domain.exception.RegraDeNegocioException;

class SugestaoTagObraTest {

    private Obra obra;
    private Usuario usuarioComum;
    private Usuario admin;
    private Usuario curador;

    @BeforeEach
    void setUp() {
        obra = new Obra("Frieren", "Sousou no Frieren", null, "Sinopse...", 100, TipoObra.MANGA);
        usuarioComum = new Usuario("user@shiori.dev", "senha", "leitor");
        admin = new Usuario("admin@shiori.dev", "senha", "admin");
        admin.promoverPara(Role.ROLE_ADMIN);
        curador = new Usuario("curador@shiori.dev", "senha", "curador");
        curador.promoverPara(Role.ROLE_CURADOR);
    }

    @Test
    @DisplayName("Deve criar sugestão de tag com status PENDENTE e validar campos obrigatórios")
    void deveCriarSugestaoPendenteEValidarCampos() {
        SugestaoTagObra sugestao = new SugestaoTagObra(obra, "Magia Ancestral", usuarioComum, "Aparece com frequência na obra");

        assertThat(sugestao.getObra()).isEqualTo(obra);
        assertThat(sugestao.getUsuarioSugeridor()).isEqualTo(usuarioComum);
        assertThat(sugestao.getNomeTag()).isEqualTo("Magia Ancestral");
        assertThat(sugestao.getJustificativa()).isEqualTo("Aparece com frequência na obra");
        assertThat(sugestao.getStatus()).isEqualTo(StatusSugestaoTag.PENDENTE);
        assertThat(sugestao.getDataSugestao()).isNotNull();
        assertThat(sugestao.isPendente()).isTrue();
        assertThat(sugestao.gerarSlug()).isEqualTo("magia-ancestral");
    }

    @Test
    @DisplayName("Deve lançar exceção ao instanciar sugestão com dados inválidos")
    void deveLancarExcecaoComDadosInvalidos() {
        assertThatThrownBy(() -> new SugestaoTagObra(null, "Tag", usuarioComum, "Justificativa"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("obra é obrigatória");

        assertThatThrownBy(() -> new SugestaoTagObra(obra, "Tag", null, "Justificativa"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("usuário autor da sugestão é obrigatório");

        assertThatThrownBy(() -> new SugestaoTagObra(obra, "   ", usuarioComum, "Justificativa"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("nome da tag ou uma tag existente deve ser informado");
    }

    @Test
    @DisplayName("Deve aprovar sugestão pendente associando a tag persistida e avaliador")
    void deveAprovarSugestaoPendente() {
        SugestaoTagObra sugestao = new SugestaoTagObra(obra, "Viagem Lenta", usuarioComum, "Foco em jornada");
        Tag tagPersistida = new Tag("Viagem Lenta", 1L);

        sugestao.aprovar(admin, tagPersistida);

        assertThat(sugestao.getStatus()).isEqualTo(StatusSugestaoTag.APROVADA);
        assertThat(sugestao.isAprovada()).isTrue();
        assertThat(sugestao.getTag()).isEqualTo(tagPersistida);
        assertThat(sugestao.getAdminAvaliador()).isEqualTo(admin);
        assertThat(sugestao.getDataAvaliacao()).isNotNull();
        assertThat(sugestao.getMotivoRejeicao()).isNull();
    }

    @Test
    @DisplayName("Não deve permitir aprovação por usuário comum ou se já avaliada")
    void naoDevePermitirAprovacaoInvalida() {
        SugestaoTagObra sugestao = new SugestaoTagObra(obra, "Fantasia Sombria", usuarioComum, "Tom escuro");
        Tag tag = new Tag("Fantasia Sombria", 1L);

        assertThatThrownBy(() -> sugestao.aprovar(null, tag))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Apenas administradores ou curadores");

        assertThatThrownBy(() -> sugestao.aprovar(usuarioComum, tag))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Apenas administradores ou curadores");

        sugestao.aprovar(admin, tag);

        // Tentativa de aprovar de novo já estando aprovada é idempotente
        sugestao.aprovar(admin, tag);
        assertThat(sugestao.isAprovada()).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar sugestão com motivo e avaliador")
    void deveRejeitarSugestaoComMotivo() {
        SugestaoTagObra sugestao = new SugestaoTagObra(obra, "Tag Ofensiva", usuarioComum, null);

        sugestao.rejeitar(curador, "Conteúdo inadequado ou redundante com o gênero");

        assertThat(sugestao.getStatus()).isEqualTo(StatusSugestaoTag.REJEITADA);
        assertThat(sugestao.isRejeitada()).isTrue();
        assertThat(sugestao.getAdminAvaliador()).isEqualTo(curador);
        assertThat(sugestao.getMotivoRejeicao()).isEqualTo("Conteúdo inadequado ou redundante com o gênero");
        assertThat(sugestao.getDataAvaliacao()).isNotNull();
    }

    @Test
    @DisplayName("Não deve permitir rejeitar sem motivo, por usuário sem permissão ou se já avaliada")
    void naoDevePermitirRejeicaoInvalida() {
        SugestaoTagObra sugestao = new SugestaoTagObra(obra, "Tag Inútil", usuarioComum, null);

        assertThatThrownBy(() -> sugestao.rejeitar(null, "Motivo"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Apenas administradores ou curadores");

        assertThatThrownBy(() -> sugestao.rejeitar(usuarioComum, "Motivo"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Apenas administradores ou curadores");

        assertThatThrownBy(() -> sugestao.rejeitar(curador, "   "))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("O motivo da rejeição é obrigatório");

        sugestao.rejeitar(curador, "Motivo válido");

        assertThatThrownBy(() -> sugestao.rejeitar(admin, "Outro motivo"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Apenas sugestões pendentes podem ser rejeitadas");
    }

    @Test
    @DisplayName("Deve sanitizar slug removendo acentos e caracteres especiais")
    void deveSanitizarSlug() {
        SugestaoTagObra sugestao = new SugestaoTagObra(obra, "Ação & Aventura Épica!!", usuarioComum, null);
        assertThat(sugestao.gerarSlug()).isEqualTo("acao-aventura-epica");
    }
}
