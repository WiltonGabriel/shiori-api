package dev.shiori.infra.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.shiori.domain.enums.Role;
import dev.shiori.domain.model.Usuario;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "ShioriUltraSecretJwtSigningKeyWithAtLeast256BitsLengthForHmacSha256Security!";
    private final long expirationMs = 3600_000L; // 1 hora

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secret, expirationMs);
    }

    @Test
    @DisplayName("Deve gerar token JWT válido e extrair claims com sucesso")
    void deveGerarETestartokenValido() {
        Usuario usuario = new Usuario("leitor.jwt@shiori.dev", "senha123");
        usuario.concluirOnboarding("leitor_jwt", "America/Sao_Paulo", null, "Bio");

        String token = jwtTokenProvider.gerarToken(usuario);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validarToken(token)).isTrue();
        assertThat(jwtTokenProvider.extrairEmail(token)).isEqualTo("leitor.jwt@shiori.dev");
        assertThat(jwtTokenProvider.extrairRole(token)).isEqualTo(Role.ROLE_USER.name());
        assertThat(jwtTokenProvider.extrairOnboardingConcluido(token)).isTrue();
    }

    @Test
    @DisplayName("Deve invalidar token malformado ou nulo")
    void deveInvalidarTokenInvalido() {
        assertThat(jwtTokenProvider.validarToken(null)).isFalse();
        assertThat(jwtTokenProvider.validarToken("")).isFalse();
        assertThat(jwtTokenProvider.validarToken("token.invalido.malformado")).isFalse();
    }

    @Test
    @DisplayName("Deve invalidar token expirado")
    void deveInvalidarTokenExpirado() {
        // Provider com 1ms de expiração
        JwtTokenProvider providerExpirado = new JwtTokenProvider(secret, 1L);
        Usuario usuario = new Usuario("expirado@shiori.dev", "senha123");

        String token = providerExpirado.gerarToken(usuario);

        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {
        }

        assertThat(providerExpirado.validarToken(token)).isFalse();
    }

    @Test
    @DisplayName("Deve falhar com erro fatal em ambiente de produção se chave for padrão ou insegura")
    void deveFalharEmProducaoComChaveInseguraOuPadrao() {
        System.setProperty("spring.profiles.active", "prod");
        try {
            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> {
                new JwtTokenProvider(JwtTokenProvider.DEFAULT_SECRET, expirationMs);
            });

            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> {
                new JwtTokenProvider("curta", expirationMs);
            });

            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> {
                new JwtTokenProvider("", expirationMs);
            });

            // Com chave segura de 32+ bytes em produção deve instanciar com sucesso
            String chaveProducaoValida = "ChaveSeguraDeProducaoComMaisDe32CaracteresHexOuTexto!";
            JwtTokenProvider providerProd = new JwtTokenProvider(chaveProducaoValida, expirationMs);
            assertThat(providerProd).isNotNull();
        } finally {
            System.clearProperty("spring.profiles.active");
        }
    }
}
