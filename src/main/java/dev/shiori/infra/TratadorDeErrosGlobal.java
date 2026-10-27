package dev.shiori.infra;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import dev.shiori.domain.exception.RecursoNaoEncontradoException;
import dev.shiori.domain.exception.RegraDeNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class TratadorDeErrosGlobal {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErrosGlobal.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail tratarRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Recurso Não Encontrado");
        problem.setType(URI.create("https://shiori.dev/erros/recurso-nao-encontrado"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ProblemDetail tratarRegraDeNegocio(RegraDeNegocioException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Regra de Negócio Violada");
        problem.setType(URI.create("https://shiori.dev/erros/regra-de-negocio"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail tratarValidacaoCampos(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Um ou mais campos contêm valores inválidos.");
        problem.setTitle("Dados de Entrada Inválidos");
        problem.setType(URI.create("https://shiori.dev/erros/validacao-campos"));
        problem.setProperty("timestamp", Instant.now());

        Map<String, String> erros = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            String mensagem = error.getDefaultMessage() != null ? error.getDefaultMessage() : "Valor inválido";
            erros.merge(error.getField(), mensagem, (msg1, msg2) -> msg1 + "; " + msg2);
        }

        problem.setProperty("campos", erros);
        return problem;
    }

    @ExceptionHandler(BindException.class)
    public ProblemDetail tratarErroBind(BindException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Um ou mais parâmetros contêm valores inválidos.");
        problem.setTitle("Parâmetros Inválidos");
        problem.setType(URI.create("https://shiori.dev/erros/parametros-invalidos"));
        problem.setProperty("timestamp", Instant.now());

        Map<String, String> erros = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            String mensagem = error.getDefaultMessage() != null ? error.getDefaultMessage() : "Valor inválido";
            erros.merge(error.getField(), mensagem, (msg1, msg2) -> msg1 + "; " + msg2);
        }

        problem.setProperty("campos", erros);
        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail tratarTipoParametroInvalido(MethodArgumentTypeMismatchException ex) {
        String detalhe = String.format("O parâmetro '%s' recebeu um valor inválido: '%s'.", ex.getName(), ex.getValue());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalhe);
        problem.setTitle("Tipo de Parâmetro Inválido");
        problem.setType(URI.create("https://shiori.dev/erros/parametro-invalido"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("parametro", ex.getName());
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail tratarMensagemIlegivel(HttpMessageNotReadableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "O corpo da requisição é inválido ou está malformado.");
        problem.setTitle("Requisição Ilegível");
        problem.setType(URI.create("https://shiori.dev/erros/requisicao-ilegivel"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ProblemDetail tratarCredenciaisInvalidas(org.springframework.security.authentication.BadCredentialsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
        problem.setTitle("Autenticação Falhou");
        problem.setType(URI.create("https://shiori.dev/erros/credenciais-invalidas"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ProblemDetail tratarAcessoNegado(org.springframework.security.access.AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Acesso proibido.");
        problem.setTitle("Acesso Proibido");
        problem.setType(URI.create("https://shiori.dev/erros/acesso-proibido"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(dev.shiori.domain.exception.MidiaNaoSuportadaException.class)
    public ProblemDetail tratarMidiaNaoSuportada(dev.shiori.domain.exception.MidiaNaoSuportadaException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
        problem.setTitle("Mídia Não Suportada");
        problem.setType(URI.create("https://shiori.dev/erros/midia-nao-suportada"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(dev.shiori.domain.exception.ArquivoInvalidoException.class)
    public ProblemDetail tratarArquivoInvalido(dev.shiori.domain.exception.ArquivoInvalidoException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Arquivo Inválido");
        problem.setType(URI.create("https://shiori.dev/erros/arquivo-invalido"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(dev.shiori.domain.exception.StorageException.class)
    public ProblemDetail tratarErroStorage(dev.shiori.domain.exception.StorageException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problem.setTitle("Falha no Armazenamento");
        problem.setType(URI.create("https://shiori.dev/erros/armazenamento"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(dev.shiori.domain.exception.IntegracaoExternaException.class)
    public ProblemDetail tratarIntegracaoExterna(dev.shiori.domain.exception.IntegracaoExternaException ex) {
        log.warn("FALHA_INTEGRACAO_EXTERNA: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setTitle("Provedor Externo Indisponível");
        problem.setType(URI.create("https://shiori.dev/erros/integracao-externa"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail tratarErroInesperado(Exception ex) {
        log.error("ERRO_INESPERADO_GLOBAL: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu uma instabilidade temporária ao processar sua solicitação. Por favor, tente novamente."
        );
        problem.setTitle("Instabilidade Temporária");
        problem.setType(URI.create("https://shiori.dev/erros/interno"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
