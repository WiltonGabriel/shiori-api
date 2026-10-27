package dev.shiori.infra.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int LIMITE_AUTH_POR_MINUTO = 30;
    private static final int LIMITE_GERAL_POR_MINUTO = 200;
    private static final long JANELA_MS = 60_000L;

    private final Map<String, RequisicoesJanela> requisicoesPorIp = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String ip = extrairIp(request);
        String uri = request.getRequestURI();
        boolean isAuth = uri.startsWith("/api/auth/") || uri.startsWith("/api/v1/auth/");
        int limite = isAuth ? LIMITE_AUTH_POR_MINUTO : LIMITE_GERAL_POR_MINUTO;

        long agora = System.currentTimeMillis();
        String chave = ip + ":" + (isAuth ? "auth" : "geral");

        RequisicoesJanela janela = requisicoesPorIp.compute(chave, (k, v) -> {
            if (v == null || agora - v.inicioJanelaMs > JANELA_MS) {
                return new RequisicoesJanela(agora, new AtomicInteger(1));
            }
            v.contador.incrementAndGet();
            return v;
        });

        if (janela.contador.get() > limite) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

            String json = String.format("""
                    {
                      "type": "https://shiori.dev/erros/limite-excedido",
                      "title": "Muitas Requisições",
                      "status": 429,
                      "detail": "Limite de requisições excedido. Por favor, aguarde alguns instantes antes de tentar novamente.",
                      "limitePorMinuto": %d,
                      "timestamp": "%s"
                    }
                    """, limite, Instant.now());

            response.getWriter().write(json);
            return;
        }

        // Limpeza periódica de entradas antigas se o mapa crescer muito
        if (requisicoesPorIp.size() > 5000) {
            requisicoesPorIp.entrySet().removeIf(e -> agora - e.getValue().inicioJanelaMs > JANELA_MS);
        }

        filterChain.doFilter(request, response);
    }

    private String extrairIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "desconhecido";
    }

    private static class RequisicoesJanela {
        final long inicioJanelaMs;
        final AtomicInteger contador;

        RequisicoesJanela(long inicioJanelaMs, AtomicInteger contador) {
            this.inicioJanelaMs = inicioJanelaMs;
            this.contador = contador;
        }
    }
}
