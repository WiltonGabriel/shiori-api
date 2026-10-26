package dev.shiori.infra.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmailCryptoConverterTest {

    private EmailCryptoConverter converter;

    @BeforeEach
    void setUp() {
        converter = new EmailCryptoConverter();
    }

    @Test
    @DisplayName("Deve encriptar e-mail em repouso no banco e decriptar de volta com fidelidade total")
    void deveEncriptarEDecriptarEmail() {
        String emailOriginal = "leitor.privado@shiori.dev";

        String ciphertext = converter.convertToDatabaseColumn(emailOriginal);

        assertThat(ciphertext).isNotNull();
        assertThat(ciphertext).isNotEqualTo(emailOriginal);
        assertThat(ciphertext).doesNotContain("leitor.privado");
        assertThat(ciphertext).doesNotContain("@shiori.dev");

        String emailDecriptado = converter.convertToEntityAttribute(ciphertext);
        assertThat(emailDecriptado).isEqualTo(emailOriginal);
    }

    @Test
    @DisplayName("Encriptação deve ser determinística para permitir integridade de índice UNIQUE e consultas exatas")
    void deveSerDeterministicaParaMesmoEmail() {
        String email = "contato@shiori.dev";

        String hash1 = converter.convertToDatabaseColumn(email);
        String hash2 = converter.convertToDatabaseColumn(email);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("Deve tratar valores nulos e em branco de forma resiliente")
    void deveTratarValoresNulosEVazios() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToDatabaseColumn("   ")).isEqualTo("   ");
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToEntityAttribute("   ")).isEqualTo("   ");
    }

    @Test
    @DisplayName("Chave customizada deve gerar cifra diferente da chave padrão")
    void deveSuportarChaveCustomizada() {
        EmailCryptoConverter customConverter = new EmailCryptoConverter("ChaveCustomizadaSuperSecreta12345");
        String email = "teste@shiori.dev";

        String cifraPadrao = converter.convertToDatabaseColumn(email);
        String cifraCustom = customConverter.convertToDatabaseColumn(email);

        assertThat(cifraPadrao).isNotEqualTo(cifraCustom);
        assertThat(customConverter.convertToEntityAttribute(cifraCustom)).isEqualTo(email);
    }

    @Test
    @DisplayName("Deve inicializar com construtor padrão e resolver chave com sucesso")
    void deveInicializarComConstrutorPadrao() {
        EmailCryptoConverter defaultConverter = new EmailCryptoConverter();
        String email = "default-teste@shiori.dev";
        String cifrado = defaultConverter.convertToDatabaseColumn(email);
        assertThat(cifrado).isNotNull().isNotEqualTo(email);
        assertThat(defaultConverter.convertToEntityAttribute(cifrado)).isEqualTo(email);
    }

    @Test
    @DisplayName("Deve inicializar com chave provida por propriedade do sistema shiori.crypto.key")
    void deveInicializarComPropriedadeDoSistema() {
        System.setProperty("shiori.crypto.key", "EmailSystemPropertyKey1234567890!");
        try {
            EmailCryptoConverter propConverter = new EmailCryptoConverter();
            String email = "prop-teste@shiori.dev";
            String cifrado = propConverter.convertToDatabaseColumn(email);
            assertThat(propConverter.convertToEntityAttribute(cifrado)).isEqualTo(email);
        } finally {
            System.clearProperty("shiori.crypto.key");
        }
    }
}
