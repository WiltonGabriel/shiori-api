package dev.shiori.dto.obra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.shiori.domain.enums.ModoFiltroColecao;
import dev.shiori.domain.exception.RegraDeNegocioException;

@DisplayName("Testes Unitários: Validação de Parâmetros da Busca Avançada (ObraBuscaAvancadaFiltro)")
class ObraBuscaAvancadaFiltroTest {

    @Test
    @DisplayName("Filtro vazio deve inicializar com valores padrão seguros")
    void deveInicializarComValoresPadrao() {
        ObraBuscaAvancadaFiltro filtro = new ObraBuscaAvancadaFiltro(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null
        );

        assertThat(filtro.generosModo()).isEqualTo(ModoFiltroColecao.AND);
        assertThat(filtro.tagsModo()).isEqualTo(ModoFiltroColecao.AND);
        assertThat(filtro.ordenacao()).isEqualTo(OrdenacaoObra.NOTA_DESC);
        assertThat(filtro.pagina()).isEqualTo(0);
        assertThat(filtro.itensPorPagina()).isEqualTo(20);

        assertThatCode(filtro::validar).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Itens por página deve ser limitado ao máximo de 50")
    void deveLimitarItensPorPaginaAoMaximo() {
        ObraBuscaAvancadaFiltro filtro = new ObraBuscaAvancadaFiltro(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, 0, 150
        );

        assertThat(filtro.itensPorPagina()).isEqualTo(50);
    }

    @Test
    @DisplayName("Deve rejeitar capítulos mínimos negativos")
    void deveRejeitarCapitulosMinimosNegativos() {
        ObraBuscaAvancadaFiltro filtro = new ObraBuscaAvancadaFiltro(
                null, null, null, null, -5, 10, null, null,
                null, null, null, null, null, null, null, 0, 20
        );

        assertThatThrownBy(filtro::validar)
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("número mínimo de capítulos não pode ser negativo");
    }

    @Test
    @DisplayName("Deve rejeitar capítulos máximos negativos")
    void deveRejeitarCapitulosMaximosNegativos() {
        ObraBuscaAvancadaFiltro filtro = new ObraBuscaAvancadaFiltro(
                null, null, null, null, 0, -10, null, null,
                null, null, null, null, null, null, null, 0, 20
        );

        assertThatThrownBy(filtro::validar)
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("número máximo de capítulos não pode ser negativo");
    }

    @Test
    @DisplayName("Deve rejeitar intervalo invertido de capítulos (mínimo > máximo)")
    void deveRejeitarIntervaloInvertidoDeCapitulos() {
        ObraBuscaAvancadaFiltro filtro = new ObraBuscaAvancadaFiltro(
                null, null, null, null, 100, 50, null, null,
                null, null, null, null, null, null, null, 0, 20
        );

        assertThatThrownBy(filtro::validar)
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("número mínimo de capítulos não pode ser maior que o máximo");
    }

    @Test
    @DisplayName("Deve rejeitar notas fora da escala de 0.0 a 10.0")
    void deveRejeitarNotasForaDaEscala() {
        ObraBuscaAvancadaFiltro notaMinInvalida = new ObraBuscaAvancadaFiltro(
                null, null, null, null, null, null, -1.0, 5.0,
                null, null, null, null, null, null, null, 0, 20
        );
        assertThatThrownBy(notaMinInvalida::validar)
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("nota mínima deve estar entre 0.0 e 10.0");

        ObraBuscaAvancadaFiltro notaMaxInvalida = new ObraBuscaAvancadaFiltro(
                null, null, null, null, null, null, 5.0, 11.0,
                null, null, null, null, null, null, null, 0, 20
        );
        assertThatThrownBy(notaMaxInvalida::validar)
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("nota máxima deve estar entre 0.0 e 10.0");
    }

    @Test
    @DisplayName("Deve rejeitar intervalo invertido de notas (mínima > máxima)")
    void deveRejeitarIntervaloInvertidoDeNotas() {
        ObraBuscaAvancadaFiltro filtro = new ObraBuscaAvancadaFiltro(
                null, null, null, null, null, null, 9.5, 8.0,
                null, null, null, null, null, null, null, 0, 20
        );

        assertThatThrownBy(filtro::validar)
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("nota mínima não pode ser maior que a nota máxima");
    }

    @Test
    @DisplayName("Filtro válido com parâmetros completos deve passar sem exceções")
    void deveAceitarFiltroValidoCompleto() {
        ObraBuscaAvancadaFiltro filtro = new ObraBuscaAvancadaFiltro(
                "Solo",
                Set.of(),
                Set.of(),
                Set.of(),
                10,
                200,
                7.0,
                9.5,
                Set.of("Ação", "Fantasia"),
                ModoFiltroColecao.AND,
                Set.of("Romance"),
                Set.of("Protagonista OP"),
                ModoFiltroColecao.OR,
                Set.of("Spoiler"),
                OrdenacaoObra.CAPITULOS_DESC,
                1,
                15
        );

        assertThatCode(filtro::validar).doesNotThrowAnyException();
        assertThat(filtro.minCapitulos()).isEqualTo(10);
        assertThat(filtro.maxCapitulos()).isEqualTo(200);
        assertThat(filtro.minNota()).isEqualTo(7.0);
        assertThat(filtro.maxNota()).isEqualTo(9.5);
    }
}
