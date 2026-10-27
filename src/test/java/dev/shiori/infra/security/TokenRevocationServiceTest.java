package dev.shiori.infra.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenRevocationServiceTest {

    private TokenRevocationService tokenRevocationService;

    @BeforeEach
    void setUp() {
        tokenRevocationService = new TokenRevocationService();
    }

    @Test
    @DisplayName("Deve revogar token e confirmar que está revogado")
    void deveRevogarEConfirmar() {
        String token = "teste.token.jwt";
        Instant expiraEm = Instant.now().plus(1, ChronoUnit.HOURS);

        assertThat(tokenRevocationService.isRevogado(token)).isFalse();

        tokenRevocationService.revogar(token, expiraEm);

        assertThat(tokenRevocationService.isRevogado(token)).isTrue();
    }

    @Test
    @DisplayName("Deve considerar token nulo ou em branco como não revogado")
    void deveIgnorarTokenNuloOuVazio() {
        assertThat(tokenRevocationService.isRevogado(null)).isFalse();
        assertThat(tokenRevocationService.isRevogado("  ")).isFalse();

        tokenRevocationService.revogar(null, Instant.now().plus(1, ChronoUnit.HOURS));
        tokenRevocationService.revogar("  ", Instant.now().plus(1, ChronoUnit.HOURS));

        assertThat(tokenRevocationService.isRevogado("")).isFalse();
    }

    @Test
    @DisplayName("Deve remover tokens já expirados na limpeza periódica")
    void deveLimparTokensExpirados() {
        String tokenExpirado = "token.expirado";
        Instant expiraNoPassado = Instant.now().minus(10, ChronoUnit.SECONDS);

        tokenRevocationService.revogar(tokenExpirado, expiraNoPassado);
        tokenRevocationService.limparExpirados();

        assertThat(tokenRevocationService.isRevogado(tokenExpirado)).isFalse();
    }
}
