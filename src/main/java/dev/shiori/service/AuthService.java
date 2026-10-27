package dev.shiori.service;

import java.util.Date;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.shiori.domain.exception.RegraDeNegocioException;
import dev.shiori.domain.model.Usuario;
import dev.shiori.dto.auth.AuthResponse;
import dev.shiori.dto.auth.LoginRequest;
import dev.shiori.dto.auth.RegistroRequest;
import dev.shiori.infra.security.JwtTokenProvider;
import dev.shiori.infra.security.TokenRevocationService;
import dev.shiori.repository.UsuarioRepository;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRevocationService tokenRevocationService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            TokenRevocationService tokenRevocationService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenRevocationService = tokenRevocationService;
    }

    @Transactional
    public AuthResponse registrar(RegistroRequest request) {
        String email = request.email().trim().toLowerCase();

        if (usuarioRepository.existsByEmail(email)) {
            throw new RegraDeNegocioException("E-mail já cadastrado na plataforma");
        }

        if (request.apelido() != null && !request.apelido().isBlank()) {
            String apelido = request.apelido().trim();
            if (usuarioRepository.existsByApelido(apelido)) {
                throw new RegraDeNegocioException("Apelido já em uso por outro usuário");
            }
        }

        String senhaCriptografada = passwordEncoder.encode(request.senha());
        Usuario usuario = new Usuario(email, senhaCriptografada);

        if (request.apelido() != null && !request.apelido().isBlank()) {
            usuario.concluirOnboarding(request.apelido().trim(), "America/Sao_Paulo", null, null);
        }

        usuario = usuarioRepository.save(usuario);
        String token = jwtTokenProvider.gerarToken(usuario);
        String refreshToken = jwtTokenProvider.gerarRefreshToken(usuario.getId(), usuario.getEmail());

        return AuthResponse.of(
                token,
                refreshToken,
                usuario.getId(),
                usuario.getEmail(),
                usuario.getApelido(),
                usuario.getRole().name(),
                usuario.isOnboardingConcluido(),
                usuario.getFormatosPreferidos());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RegraDeNegocioException("Credenciais inválidas"));

        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new RegraDeNegocioException("Credenciais inválidas");
        }

        if (!usuario.isAtivo()) {
            throw new RegraDeNegocioException("Esta conta foi desativada ou anonimizada");
        }

        boolean lembrar = Boolean.TRUE.equals(request.lembrarDeMim());
        String token = lembrar ? jwtTokenProvider.gerarToken(usuario, true) : jwtTokenProvider.gerarToken(usuario);
        String refreshToken = jwtTokenProvider.gerarRefreshToken(usuario.getId(), usuario.getEmail());

        return AuthResponse.of(
                token,
                refreshToken,
                usuario.getId(),
                usuario.getEmail(),
                usuario.getApelido(),
                usuario.getRole().name(),
                usuario.isOnboardingConcluido(),
                usuario.getFormatosPreferidos());
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenStr) {
        if (!jwtTokenProvider.validarToken(refreshTokenStr) || !jwtTokenProvider.isRefreshToken(refreshTokenStr)) {
            throw new RegraDeNegocioException("Refresh token inválido ou expirado");
        }
        if (tokenRevocationService.isRevogado(refreshTokenStr)) {
            throw new RegraDeNegocioException("Refresh token revogado. Por favor, autentique-se novamente.");
        }

        Long usuarioId = jwtTokenProvider.extrairUsuarioId(refreshTokenStr);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário não encontrado"));

        if (!usuario.isAtivo()) {
            throw new RegraDeNegocioException("Esta conta foi desativada");
        }

        // Rotação: revoga o refresh token antigo
        Date expiraEm = jwtTokenProvider.extrairExpiracao(refreshTokenStr);
        tokenRevocationService.revogarToken(refreshTokenStr, expiraEm.getTime());

        // Gera novo par rotativo
        String novoAccessToken = jwtTokenProvider.gerarToken(usuario);
        String novoRefreshToken = jwtTokenProvider.gerarRefreshToken(usuario.getId(), usuario.getEmail());

        return AuthResponse.of(
                novoAccessToken,
                novoRefreshToken,
                usuario.getId(),
                usuario.getEmail(),
                usuario.getApelido(),
                usuario.getRole().name(),
                usuario.isOnboardingConcluido(),
                usuario.getFormatosPreferidos());
    }

    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null && jwtTokenProvider.validarToken(accessToken)) {
            Date expiraEm = jwtTokenProvider.extrairExpiracao(accessToken);
            tokenRevocationService.revogarToken(accessToken, expiraEm.getTime());
        }
        if (refreshToken != null && jwtTokenProvider.validarToken(refreshToken)) {
            Date expiraEm = jwtTokenProvider.extrairExpiracao(refreshToken);
            tokenRevocationService.revogarToken(refreshToken, expiraEm.getTime());
        }
    }
}
