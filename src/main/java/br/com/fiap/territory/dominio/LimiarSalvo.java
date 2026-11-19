package br.com.fiap.territory.dominio;

import br.com.fiap.territory.excecao.DadosInvalidosException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Limiar alterado pelo menu e gravado no banco (tabela LIMIAR), para valer também depois de reiniciar.
 * Há no máximo uma linha por tipo de sensor: gravar de novo substitui a anterior.
 * Tipos sem linha aqui usam o padrão do ConfiguracaoSistema.
 */
@Entity
@Table(name = "limiar")
public class LimiarSalvo {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_sensor", length = 20)
    private TipoSensor tipoSensor;

    @Column(nullable = false)
    private double atencao;

    @Column(nullable = false)
    private double alerta;

    @Column(nullable = false)
    private double critico;

    @Column(name = "alterado_em", nullable = false)
    private LocalDateTime alteradoEm;

    /** Exigido pelo JPA. Não usar no código da aplicação. */
    protected LimiarSalvo() {
    }

    public LimiarSalvo(TipoSensor tipoSensor, Limiar limiar, LocalDateTime alteradoEm) {
        this.tipoSensor = Objects.requireNonNull(tipoSensor, "tipoSensor");
        Objects.requireNonNull(limiar, "limiar");
        this.atencao = limiar.getAtencao();
        this.alerta = limiar.getAlerta();
        this.critico = limiar.getCritico();
        this.alteradoEm = Objects.requireNonNull(alteradoEm, "alteradoEm");
    }

    /**
     * Converte de volta para o objeto de domínio, passando de novo pela validação do Limiar.
     *
     * @throws DadosInvalidosException se a linha do banco foi editada à mão com valores inválidos
     */
    public Limiar paraLimiar() throws DadosInvalidosException {
        return new Limiar(atencao, alerta, critico);
    }

    public TipoSensor getTipoSensor() {
        return tipoSensor;
    }

    public LocalDateTime getAlteradoEm() {
        return alteradoEm;
    }
}
