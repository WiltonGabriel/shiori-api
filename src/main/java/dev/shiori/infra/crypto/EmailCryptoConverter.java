package dev.shiori.infra.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EmailCryptoConverter implements AttributeConverter<String, String> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmailCryptoConverter.class);
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String SECRET_KEY_DEFAULT = "ShioriLgpdSecureAes256SecretKey!"; // 32 bytes = 256 bits

    private final byte[] keyBytes;

    public EmailCryptoConverter() {
        String envKey = resolverChaveAmbiente();
        if (envKey != null && !envKey.isBlank()) {
            this.keyBytes = gerarChave32Bytes(envKey.trim());
        } else {
            if (isAmbienteProducao()) {
                throw new IllegalStateException("ERRO_FATAL_SEGURANCA: A chave de criptografia SHIORI_CRYPTO_KEY (ou SHIORI_CRYPTO_SECRET_KEY) é obrigatória em ambiente de produção!");
            }
            log.warn("AVISO_SEGURANCA: SHIORI_CRYPTO_KEY não configurada. Usando chave padrão para desenvolvimento local/testes.");
            this.keyBytes = SECRET_KEY_DEFAULT.getBytes(StandardCharsets.UTF_8);
        }
    }

    public EmailCryptoConverter(String chaveSecreta) {
        if (chaveSecreta == null || chaveSecreta.isBlank()) {
            this.keyBytes = SECRET_KEY_DEFAULT.getBytes(StandardCharsets.UTF_8);
        } else {
            this.keyBytes = gerarChave32Bytes(chaveSecreta.trim());
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

    private static byte[] gerarChave32Bytes(String input) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return sha.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao derivar chave SHA-256", e);
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) {
            return attribute;
        }

        try {
            // IV determinístico derivado do hash SHA-256 do atributo (16 bytes)
            // Garante unicidade e buscas exatas via JPA mantendo encriptação forte em repouso
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(attribute.getBytes(StandardCharsets.UTF_8));
            byte[] iv = Arrays.copyOf(hash, 16);

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            byte[] encrypted = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao encriptar e-mail para persistência", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return dbData;
        }

        try {
            byte[] combined = Base64.getDecoder().decode(dbData);
            if (combined.length <= 16) {
                // Se não contiver ao menos 16 bytes de IV + payload, retorna original por segurança
                return dbData;
            }

            byte[] iv = Arrays.copyOfRange(combined, 0, 16);
            byte[] cipherText = Arrays.copyOfRange(combined, 16, combined.length);

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] decrypted = cipher.doFinal(cipherText);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Em caso de dado plain-text legado (fallback resiliente)
            return dbData;
        }
    }
}
