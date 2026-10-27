package dev.shiori.domain.model;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

import dev.shiori.domain.enums.StatusSugestaoTag;
import dev.shiori.domain.exception.RegraDeNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_sugestao_tag_obra")
public class SugestaoTagObra extends EntidadeBase {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obra_id", nullable = false)
    private Obra obra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id")
    private Tag tag;

    @Column(name = "nome_tag", nullable = false, length = 60)
    private String nomeTag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_sugeridor_id", nullable = false)
    private Usuario usuarioSugeridor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusSugestaoTag status = StatusSugestaoTag.PENDENTE;

    @Column(length = 255)
    private String justificativa;

    @Column(name = "data_sugestao", nullable = false)
    private LocalDateTime dataSugestao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_avaliador_id")
    private Usuario adminAvaliador;

    @Column(name = "data_avaliacao")
    private LocalDateTime dataAvaliacao;

    @Column(name = "motivo_rejeicao", length = 255)
    private String motivoRejeicao;

    protected SugestaoTagObra() {
    }

    public SugestaoTagObra(Obra obra, Tag tag, String nomeTag, Usuario usuarioSugeridor, String justificativa) {
        if (obra == null) {
            throw new RegraDeNegocioException("A obra é obrigatória");
        }
        if (usuarioSugeridor == null) {
            throw new RegraDeNegocioException("O usuário autor da sugestão é obrigatório");
        }
        if ((nomeTag == null || nomeTag.trim().isBlank()) && tag == null) {
            throw new RegraDeNegocioException("O nome da tag ou uma tag existente deve ser informado");
        }

        this.obra = obra;
        this.tag = tag;
        this.nomeTag = tag != null ? tag.getNome() : nomeTag.trim();
        if (this.nomeTag.length() > 60) {
            throw new RegraDeNegocioException("O nome da tag não pode exceder 60 caracteres");
        }
        this.usuarioSugeridor = usuarioSugeridor;
        this.justificativa = (justificativa != null && !justificativa.trim().isBlank())
                ? justificativa.trim()
                : null;
        if (this.justificativa != null && this.justificativa.length() > 255) {
            throw new RegraDeNegocioException("A justificativa não pode exceder 255 caracteres");
        }
        this.status = StatusSugestaoTag.PENDENTE;
        this.dataSugestao = LocalDateTime.now();
    }

    public SugestaoTagObra(Obra obra, String nomeTag, Usuario usuarioSugeridor, String justificativa) {
        this(obra, null, nomeTag, usuarioSugeridor, justificativa);
    }

    public boolean isPendente() {
        return this.status == StatusSugestaoTag.PENDENTE;
    }

    public boolean isAprovada() {
        return this.status == StatusSugestaoTag.APROVADA;
    }

    public boolean isRejeitada() {
        return this.status == StatusSugestaoTag.REJEITADA;
    }

    public void aprovar(Usuario avaliador, Tag tagAprovada) {
        if (avaliador == null || !avaliador.isCurador()) {
            throw new RegraDeNegocioException("Apenas administradores ou curadores podem aprovar sugestões de tags");
        }
        if (this.status == StatusSugestaoTag.APROVADA) {
            return; // Idempotente
        }
        if (this.status != StatusSugestaoTag.PENDENTE) {
            throw new RegraDeNegocioException("Apenas sugestões pendentes podem ser aprovadas");
        }
        if (tagAprovada != null) {
            this.tag = tagAprovada;
        }
        this.status = StatusSugestaoTag.APROVADA;
        this.adminAvaliador = avaliador;
        this.dataAvaliacao = LocalDateTime.now();
        this.motivoRejeicao = null;
    }

    public void rejeitar(Usuario avaliador, String motivo) {
        if (avaliador == null || !avaliador.isCurador()) {
            throw new RegraDeNegocioException("Apenas administradores ou curadores podem rejeitar sugestões de tags");
        }
        if (this.status != StatusSugestaoTag.PENDENTE) {
            throw new RegraDeNegocioException("Apenas sugestões pendentes podem ser rejeitadas");
        }
        if (motivo == null || motivo.trim().isBlank()) {
            throw new RegraDeNegocioException("O motivo da rejeição é obrigatório");
        }
        this.status = StatusSugestaoTag.REJEITADA;
        this.motivoRejeicao = motivo.trim();
        this.adminAvaliador = avaliador;
        this.dataAvaliacao = LocalDateTime.now();
    }

    public String gerarSlug() {
        String nowhitespace = WHITESPACE.matcher(this.nomeTag.trim().toLowerCase()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        return slug.replaceAll("-+", "-");
    }

    public Obra getObra() {
        return obra;
    }

    public Tag getTag() {
        return tag;
    }

    public String getNomeTag() {
        return nomeTag;
    }

    public Usuario getUsuarioSugeridor() {
        return usuarioSugeridor;
    }

    public StatusSugestaoTag getStatus() {
        return status;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public LocalDateTime getDataSugestao() {
        return dataSugestao;
    }

    public Usuario getAdminAvaliador() {
        return adminAvaliador;
    }

    public LocalDateTime getDataAvaliacao() {
        return dataAvaliacao;
    }

    public String getMotivoRejeicao() {
        return motivoRejeicao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SugestaoTagObra that = (SugestaoTagObra) o;
        if (getId() != null && that.getId() != null) {
            return getId().equals(that.getId());
        }
        return Objects.equals(obra, that.obra)
                && Objects.equals(nomeTag, that.nomeTag)
                && status == that.status;
    }

    @Override
    public int hashCode() {
        if (getId() != null) {
            return getId().hashCode();
        }
        return Objects.hash(obra, nomeTag, status);
    }
}
