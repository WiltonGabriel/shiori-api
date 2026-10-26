package dev.shiori.service.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.shiori.domain.exception.RecursoNaoEncontradoException;
import dev.shiori.domain.exception.RegraDeNegocioException;
import dev.shiori.domain.model.RegistroAuditoria;
import dev.shiori.domain.model.Usuario;
import dev.shiori.dto.admin.RegistroAuditoriaResponse;
import dev.shiori.repository.RegistroAuditoriaRepository;
import dev.shiori.repository.UsuarioRepository;

/**
 * Serviço responsável exclusivamente pela auditoria de segurança e validação de privilégios de administradores.
 * Adere estritamente ao Princípio da Responsabilidade Única (SRP) e Clean Code.
 */
@Service
@Transactional
public class AdminAuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuditoriaService.class);

    private final UsuarioRepository usuarioRepository;
    private final RegistroAuditoriaRepository registroAuditoriaRepository;

    public AdminAuditoriaService(UsuarioRepository usuarioRepository,
                                 RegistroAuditoriaRepository registroAuditoriaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.registroAuditoriaRepository = registroAuditoriaRepository;
    }

    public Usuario obterEValidarAdmin(Long adminId) {
        if (adminId == null) {
            throw new RegraDeNegocioException("ID do administrador não informado");
        }
        Usuario admin = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Administrador não encontrado: ID " + adminId));

        if (!admin.isAdmin()) {
            throw new RegraDeNegocioException("Acesso negado: operação restrita a administradores");
        }
        return admin;
    }

    public Usuario obterEValidarCuradorOuAdmin(Long operadorId) {
        if (operadorId == null) {
            throw new RegraDeNegocioException("ID do operador não informado");
        }
        Usuario operador = usuarioRepository.findById(operadorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário operador não encontrado: ID " + operadorId));

        if (!operador.isCurador()) {
            throw new RegraDeNegocioException("Acesso negado: operação restrita a curadores ou administradores");
        }
        return operador;
    }

    public void registrarAuditoria(Usuario admin, String acao, String entidadeAfetada, String entidadeId, String detalhes) {
        RegistroAuditoria registro = new RegistroAuditoria(
                admin.getId(),
                admin.getApelidoOuEmail(),
                acao,
                entidadeAfetada,
                entidadeId,
                detalhes,
                "127.0.0.1"
        );
        registroAuditoriaRepository.save(registro);

        log.warn("AUDITORIA_SEGURANCA: Admin [id={}, apelido={}] executou [{}] sobre [{} id={}]. Detalhes: {}",
                admin.getId(), admin.getApelidoOuEmail(), acao, entidadeAfetada, entidadeId, detalhes);
    }

    @Transactional(readOnly = true)
    public Page<RegistroAuditoriaResponse> listarAuditoria(Pageable pageable, Long adminId) {
        obterEValidarAdmin(adminId);
        return registroAuditoriaRepository.findAllByOrderByDataHoraDesc(pageable)
                .map(RegistroAuditoriaResponse::deEntidade);
    }
}
