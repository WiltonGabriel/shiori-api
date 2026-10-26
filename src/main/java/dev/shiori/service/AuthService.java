package dev.shiori.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.shiori.domain.exception.RegraDeNegocioException;
import dev.shiori.domain.model.Usuario;
import dev.shiori.dto.auth.AuthResponse;
import dev.shiori.dto.auth.LoginRequest;
import dev.shiori.dto.auth.RegistroRequest;
import dev.shiori.infra.security.JwtTokenProvider;
import dev.shiori.repository.UsuarioRepository;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
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

        return AuthResponse.of(
                token,
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

        return AuthResponse.of(
                token,
                usuario.getId(),
                usuario.getEmail(),
                usuario.getApelido(),
                usuario.getRole().name(),
                usuario.isOnboardingConcluido(),
                usuario.getFormatosPreferidos());
    }
}
