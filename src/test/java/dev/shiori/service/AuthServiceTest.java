package dev.shiori.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import dev.shiori.domain.exception.RegraDeNegocioException;
import dev.shiori.domain.model.Usuario;
import dev.shiori.dto.auth.AuthResponse;
import dev.shiori.dto.auth.LoginRequest;
import dev.shiori.dto.auth.RegistroRequest;
import dev.shiori.infra.security.JwtTokenProvider;
import dev.shiori.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(usuarioRepository, passwordEncoder, jwtTokenProvider);
    }

    @Test
    @DisplayName("Deve registrar novo usuário com sucesso e gerar token")
    void deveRegistrarUsuarioComSucesso() {
        RegistroRequest request = new RegistroRequest("novo@shiori.dev", "senhaForte123", "novo_leitor");

        when(usuarioRepository.existsByEmail("novo@shiori.dev")).thenReturn(false);
        when(usuarioRepository.existsByApelido("novo_leitor")).thenReturn(false);
        when(passwordEncoder.encode("senhaForte123")).thenReturn("hashBcrypt123");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtTokenProvider.gerarToken(any(Usuario.class))).thenReturn("jwt.token.mockado");

        AuthResponse response = authService.registrar(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("jwt.token.mockado");
        assertThat(response.email()).isEqualTo("novo@shiori.dev");
        assertThat(response.apelido()).isEqualTo("novo_leitor");
        assertThat(response.onboardingConcluido()).isTrue();
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve rejeitar registro com e-mail duplicado")
    void deveRejeitarEmailDuplicado() {
        RegistroRequest request = new RegistroRequest("duplicado@shiori.dev", "senha123", null);

        when(usuarioRepository.existsByEmail("duplicado@shiori.dev")).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("E-mail já cadastrado");
    }

    @Test
    @DisplayName("Deve rejeitar registro com apelido duplicado")
    void deveRejeitarApelidoDuplicado() {
        RegistroRequest request = new RegistroRequest("novo2@shiori.dev", "senha123", "apelido_existente");

        when(usuarioRepository.existsByEmail("novo2@shiori.dev")).thenReturn(false);
        when(usuarioRepository.existsByApelido("apelido_existente")).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Apelido já em uso");
    }

    @Test
    @DisplayName("Deve realizar login com credenciais válidas e devolver token")
    void deveRealizarLoginComSucesso() {
        Usuario usuario = new Usuario("login@shiori.dev", "hashBcrypt");
        LoginRequest request = new LoginRequest("login@shiori.dev", "senhaCorreta");

        when(usuarioRepository.findByEmail("login@shiori.dev")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaCorreta", "hashBcrypt")).thenReturn(true);
        when(jwtTokenProvider.gerarToken(usuario)).thenReturn("jwt.login.token");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("jwt.login.token");
        assertThat(response.email()).isEqualTo("login@shiori.dev");
    }

    @Test
    @DisplayName("Deve realizar login com sucesso e gerar token com validade estendida quando lembrarDeMim for true")
    void deveRealizarLoginComLembrarDeMim() {
        Usuario usuario = new Usuario("login@shiori.dev", "hashBcrypt");
        LoginRequest request = new LoginRequest("login@shiori.dev", "senhaCorreta", true);

        when(usuarioRepository.findByEmail("login@shiori.dev")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaCorreta", "hashBcrypt")).thenReturn(true);
        when(jwtTokenProvider.gerarToken(usuario, true)).thenReturn("jwt.persistent.token");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("jwt.persistent.token");
        assertThat(response.email()).isEqualTo("login@shiori.dev");
    }

    @Test
    @DisplayName("Deve falhar no login com senha incorreta")
    void deveFalharLoginComSenhaIncorreta() {
        Usuario usuario = new Usuario("login@shiori.dev", "hashBcrypt");
        LoginRequest request = new LoginRequest("login@shiori.dev", "senhaErrada");

        when(usuarioRepository.findByEmail("login@shiori.dev")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaErrada", "hashBcrypt")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Credenciais inválidas");
    }

    @Test
    @DisplayName("Deve falhar no login de usuário desativado")
    void deveFalharLoginDeUsuarioDesativado() {
        Usuario usuario = new Usuario("inativo@shiori.dev", "hashBcrypt");
        usuario.desativarConta();
        LoginRequest request = new LoginRequest("inativo@shiori.dev", "senhaCorreta");

        when(usuarioRepository.findByEmail("inativo@shiori.dev")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaCorreta", "hashBcrypt")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("desativada");
    }
}
