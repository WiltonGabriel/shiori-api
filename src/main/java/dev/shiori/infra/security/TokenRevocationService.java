package dev.shiori.infra.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * Serviço de revogação de tokens JWT (Blacklist com TTL automático).
 * Suporta logout real e invalidação imediata de tokens de acesso e refresh.
 */
@Service
public class TokenRevocationService {

    // Guarda token -> timestamp limite de expiração em milissegundos
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    /**
     * Revoga um token explicitamente até seu momento natural de expiração.
     */
    public void revogarToken(String token, long expiraEmMs) {
        if (token != null && !token.isBlank()) {
            blacklist.put(token, expiraEmMs);
            purgarExpiradosSeNecessario();
        }
    }

    public void revogarToken(String token, java.time.Instant expiraEm) {
        if (expiraEm != null) {
            revogarToken(token, expiraEm.toEpochMilli());
        }
    }

    public void revogar(String token, long expiraEmMs) {
        revogarToken(token, expiraEmMs);
    }

    public void revogar(String token, java.time.Instant expiraEm) {
        revogarToken(token, expiraEm);
    }

    public void limparExpirados() {
        long agora = System.currentTimeMillis();
        blacklist.entrySet().removeIf(entry -> agora > entry.getValue());
    }

    /**
     * Verifica se o token informado foi revogado (consta na blacklist e ainda não expirou).
     */
    public boolean isRevogado(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Long expiraEm = blacklist.get(token);
        if (expiraEm == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiraEm) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    private void purgarExpiradosSeNecessario() {
        if (blacklist.size() > 5000) {
            long agora = System.currentTimeMillis();
            blacklist.entrySet().removeIf(entry -> agora > entry.getValue());
        }
    }

    public void limparParaTestes() {
        blacklist.clear();
    }
}
