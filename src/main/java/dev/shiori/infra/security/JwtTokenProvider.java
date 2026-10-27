package dev.shiori.infra.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.shiori.domain.model.Usuario;
import dev.shiori.infra.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    public static final String DEFAULT_SECRET = "ShioriUltraSecretJwtSigningKeyWithAtLeast256BitsLengthForHmacSha256Security!";

    private final long expirationMs;
    private final SecretKey key;

    @Autowired
    public JwtTokenProvider(JwtProperties jwtProperties) {
        this(jwtProperties.getSecret(), jwtProperties.getExpirationMs());
    }

    public JwtTokenProvider(String secret, long expirationMs) {
        validarChaveSegura(secret);
        this.expirationMs = expirationMs;
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private static void validarChaveSegura(String secret) {
        if (isAmbienteProducao()) {
            if (secret == null || secret.isBlank() || DEFAULT_SECRET.equals(secret) || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalStateException("ERRO_FATAL_SEGURANCA: A chave JWT (JWT_SECRET ou SHIORI_JWT_SECRET) é obrigatória, não pode ser a padrão e deve conter pelo menos 32 caracteres (256 bits) em ambiente de produção!");
            }
        } else if (DEFAULT_SECRET.equals(secret)) {
            log.warn("AVISO_SEGURANCA: Usando chave JWT padrão para desenvolvimento/testes. Em produção, defina JWT_SECRET.");
        }
    }

    private static boolean isAmbienteProducao() {
        String profile = System.getenv("SPRING_PROFILES_ACTIVE");
        if (profile == null || profile.isBlank()) {
            profile = System.getProperty("spring.profiles.active");
        }
        return profile != null && (profile.equalsIgnoreCase("prod") || profile.equalsIgnoreCase("production"));
    }

    public String gerarToken(Usuario usuario) {
        return gerarToken(usuario, false);
    }

    public String gerarToken(Usuario usuario, boolean lembrarDeMim) {
        return gerarToken(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getRole() != null ? usuario.getRole().name() : "ROLE_USER",
                usuario.isOnboardingConcluido(),
                lembrarDeMim);
    }

    public String gerarToken(Long usuarioId, String email, String role, boolean onboardingConcluido) {
        return gerarToken(usuarioId, email, role, onboardingConcluido, false);
    }

    public String gerarToken(Long usuarioId, String email, String role, boolean onboardingConcluido, boolean lembrarDeMim) {
        Date agora = new Date();
        long tempoExpiracao = lembrarDeMim ? 30L * 24 * 60 * 60 * 1000 : expirationMs;
        Date expiracao = new Date(agora.getTime() + tempoExpiracao);

        return Jwts.builder()
                .subject(email)
                .claim("id", usuarioId)
                .claim("role", role)
                .claim("onboardingConcluido", onboardingConcluido)
                .claim("tokenType", "ACCESS")
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(key)
                .compact();
    }

    public String gerarRefreshToken(Long usuarioId, String email) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + 30L * 24 * 60 * 60 * 1000L);

        return Jwts.builder()
                .subject(email)
                .claim("id", usuarioId)
                .claim("tokenType", "REFRESH")
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(key)
                .compact();
    }

    public Date extrairExpiracao(String token) {
        return extrairClaims(token).getExpiration();
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims claims = extrairClaims(token);
            return "REFRESH".equals(claims.get("tokenType"));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validarToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extrairEmail(String token) {
        return extrairClaims(token).getSubject();
    }

    public Long extrairUsuarioId(String token) {
        Object idObj = extrairClaims(token).get("id");
        if (idObj instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public String extrairRole(String token) {
        Object roleObj = extrairClaims(token).get("role");
        return roleObj != null ? roleObj.toString() : "ROLE_USER";
    }

    public boolean extrairOnboardingConcluido(String token) {
        Object onboarding = extrairClaims(token).get("onboardingConcluido");
        return Boolean.TRUE.equals(onboarding);
    }
}
