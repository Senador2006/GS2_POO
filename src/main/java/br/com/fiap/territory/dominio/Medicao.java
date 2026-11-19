package br.com.fiap.territory.dominio;

import br.com.fiap.territory.excecao.DadosInvalidosException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Uma leitura de sensor gravada no banco (tabela MEDICAO).
 * Só é possível criar uma Medicao válida: o construtor rejeita valores fora da faixa física.
 */
@Entity
@Table(name = "medicao")
public class Medicao {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_sensor", nullable = false, length = 20)
    private String codigoSensor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_sensor", nullable = false, length = 20)
    private TipoSensor tipoSensor;

    @Column(nullable = false)
    private double valor;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    /** Exigido pelo JPA. Não usar no código da aplicação. */
    protected Medicao() {
    }

    public Medicao(String codigoSensor, TipoSensor tipoSensor, double valor, LocalDateTime dataHora)
            throws DadosInvalidosException {
        if (codigoSensor == null || codigoSensor.isBlank()) {
            throw new DadosInvalidosException("medição sem código de sensor");
        }
        if (tipoSensor == null) {
            throw new DadosInvalidosException("medição do sensor " + codigoSensor + " sem tipo");
        }
        if (dataHora == null) {
            throw new DadosInvalidosException("medição do sensor " + codigoSensor + " sem data/hora");
        }
        tipoSensor.validar(valor);
        this.codigoSensor = codigoSensor;
        this.tipoSensor = tipoSensor;
        this.valor = valor;
        this.dataHora = dataHora;
    }

    public Long getId() {
        return id;
    }

    public String getCodigoSensor() {
        return codigoSensor;
    }

    public TipoSensor getTipoSensor() {
        return tipoSensor;
    }

    public double getValor() {
        return valor;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    @Override
    public String toString() {
        return String.format("%s | %s | %.2f %s", dataHora.format(FORMATO), codigoSensor,
                valor, tipoSensor.getUnidade());
    }
}
