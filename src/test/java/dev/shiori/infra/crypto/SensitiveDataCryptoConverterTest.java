package dev.shiori.infra.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SensitiveDataCryptoConverterTest {

    private SensitiveDataCryptoConverter converter;

    @BeforeEach
    void setUp() {
        converter = new SensitiveDataCryptoConverter("ChaveDeTesteSeguraParaSensitiveData123!");
    }

    @Test
    @DisplayName("Deve encriptar e decriptar dado sensível com AES-256-GCM com sucesso")
    void deveEncriptarEDecriptarDadoSensivel() {
        String urlOriginal = "https://scanprivada.dev/capitulos/berserk/150";

        String criptografado = converter.convertToDatabaseColumn(urlOriginal);
        assertThat(criptografado).isNotNull();
        assertThat(criptografado).isNotEqualTo(urlOriginal);

        String decriptado = converter.convertToEntityAttribute(criptografado);
        assertThat(decriptado).isEqualTo(urlOriginal);
    }

    @Test
    @DisplayName("Deve gerar cifras diferentes para o mesmo texto (IV aleatório por operação)")
    void deveGerarCifrasDiferentesComIvAleatorio() {
        String texto = "https://scan.dev/cap1";

        String cifra1 = converter.convertToDatabaseColumn(texto);
        String cifra2 = converter.convertToDatabaseColumn(texto);

        assertThat(cifra1).isNotEqualTo(cifra2);
        assertThat(converter.convertToEntityAttribute(cifra1)).isEqualTo(texto);
        assertThat(converter.convertToEntityAttribute(cifra2)).isEqualTo(texto);
    }

    @Test
    @DisplayName("Deve lidar graciosamente com valores nulos ou vazios")
    void deveLidarComValoresNulosOuVazios() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToDatabaseColumn("")).isEqualTo("");
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToEntityAttribute("")).isEqualTo("");
    }

    @Test
    @DisplayName("Deve inicializar com construtor padrão e resolver chave com sucesso")
    void deveInicializarComConstrutorPadrao() {
        SensitiveDataCryptoConverter defaultConverter = new SensitiveDataCryptoConverter();
        String texto = "https://leitor.dev/capitulo/1";
        String cifrado = defaultConverter.convertToDatabaseColumn(texto);
        assertThat(cifrado).isNotNull().isNotEqualTo(texto);
        assertThat(defaultConverter.convertToEntityAttribute(cifrado)).isEqualTo(texto);
    }

    @Test
    @DisplayName("Deve inicializar com chave provida por propriedade do sistema shiori.crypto.key")
    void deveInicializarComPropriedadeDoSistema() {
        System.setProperty("shiori.crypto.key", "ChaveCustomizadaSystemProperty12345");
        try {
            SensitiveDataCryptoConverter propConverter = new SensitiveDataCryptoConverter();
            String texto = "dado-sensivel-teste";
            String cifrado = propConverter.convertToDatabaseColumn(texto);
            assertThat(propConverter.convertToEntityAttribute(cifrado)).isEqualTo(texto);
        } finally {
            System.clearProperty("shiori.crypto.key");
        }
    }
}
