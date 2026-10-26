package dev.shiori.infra.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class SensitiveDataCryptoConverter implements AttributeConverter<String, String> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SensitiveDataCryptoConverter.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;
    private static final String SECRET_KEY_DEFAULT = "ShioriSensitiveDataAes256SecretKey!";

    private final byte[] keyBytes;
    private final SecureRandom secureRandom = new SecureRandom();

    public SensitiveDataCryptoConverter() {
        String envKey = resolverChaveAmbiente();
        if (envKey != null && !envKey.isBlank()) {
            this.keyBytes = derivarChave32Bytes(envKey.trim());
        } else {
            if (isAmbienteProducao()) {
                throw new IllegalStateException("ERRO_FATAL_SEGURANCA: A chave de criptografia SHIORI_CRYPTO_KEY (ou SHIORI_CRYPTO_SECRET_KEY) é obrigatória em ambiente de produção!");
            }
            log.warn("AVISO_SEGURANCA: SHIORI_CRYPTO_KEY não configurada. Usando chave padrão para desenvolvimento local/testes.");
            this.keyBytes = derivarChave32Bytes(SECRET_KEY_DEFAULT);
        }
    }

    public SensitiveDataCryptoConverter(String chaveSecreta) {
        if (chaveSecreta == null || chaveSecreta.isBlank()) {
            this.keyBytes = derivarChave32Bytes(SECRET_KEY_DEFAULT);
        } else {
            this.keyBytes = derivarChave32Bytes(chaveSecreta.trim());
        }
    }

    private static String resolverChaveAmbiente() {
        String key = System.getenv("SHIORI_CRYPTO_KEY");
        if (key == null || key.isBlank()) {
            key = System.getenv("SHIORI_CRYPTO_SECRET_KEY");
        }
        if (key == null || key.isBlank()) {
            key = System.getProperty("shiori.crypto.key");
        }
        if (key == null || key.isBlank()) {
            key = System.getProperty("SHIORI_CRYPTO_KEY");
        }
        return key;
    }

    private static boolean isAmbienteProducao() {
        String profile = System.getenv("SPRING_PROFILES_ACTIVE");
        if (profile == null || profile.isBlank()) {
            profile = System.getProperty("spring.profiles.active");
        }
        return profile != null && (profile.equalsIgnoreCase("prod") || profile.equalsIgnoreCase("production"));
    }

    private static byte[] derivarChave32Bytes(String input) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return sha.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao derivar chave criptográfica SHA-256", e);
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) {
            return attribute;
        }

        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] cipherText = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao encriptar dado sensível com AES-GCM", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return dbData;
        }

        try {
            byte[] combined = Base64.getDecoder().decode(dbData);
            if (combined.length <= IV_LENGTH_BYTES) {
                return dbData;
            }

            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH_BYTES);
            byte[] cipherText = Arrays.copyOfRange(combined, IV_LENGTH_BYTES, combined.length);

            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Em caso de dado plain-text legado (fallback resiliente)
            return dbData;
        }
    }
}
