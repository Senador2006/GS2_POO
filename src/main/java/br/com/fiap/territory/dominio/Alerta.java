package br.com.fiap.territory.dominio;

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
import java.util.Objects;

/**
 * Alerta emitido quando uma previsão ultrapassa um limiar (tabela ALERTA).
 * Guarda tudo o que é preciso para EXPLICAR o alerta: qual sensor, valor previsto,
 * limite estourado e qual estratégia de previsão gerou o número.
 */
@Entity
@Table(name = "alerta")
public class Alerta {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_sensor", nullable = false, length = 20)
    private String codigoSensor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_sensor", nullable = false, length = 20)
    private TipoSensor tipoSensor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NivelAlerta nivel;

    @Column(name = "valor_previsto", nullable = false)
    private double valorPrevisto;

    @Column(name = "limite_ultrapassado", nullable = false)
    private double limiteUltrapassado;

    @Column(nullable = false, length = 60)
    private String estrategia;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    /** Exigido pelo JPA. Não usar no código da aplicação. */
    protected Alerta() {
    }

    public Alerta(String codigoSensor, TipoSensor tipoSensor, NivelAlerta nivel, double valorPrevisto,
                  double limiteUltrapassado, String estrategia, LocalDateTime dataHora) {
        this.codigoSensor = Objects.requireNonNull(codigoSensor, "codigoSensor");
        this.tipoSensor = Objects.requireNonNull(tipoSensor, "tipoSensor");
        this.nivel = Objects.requireNonNull(nivel, "nivel");
        this.valorPrevisto = valorPrevisto;
        this.limiteUltrapassado = limiteUltrapassado;
        this.estrategia = Objects.requireNonNull(estrategia, "estrategia");
        this.dataHora = Objects.requireNonNull(dataHora, "dataHora");
    }

    /** Texto que explica o porquê do alerta para quem vai decidir. */
    public String explicar() {
        String unidade = tipoSensor.getUnidade();
        return String.format("[%s] sensor %s (%s): previsão de %.2f %s pela %s ultrapassa o limite de %.2f %s. "
                        + "Sugestão: %s",
                nivel.getRotulo(), codigoSensor, tipoSensor.getDescricao(), valorPrevisto, unidade,
                estrategia, limiteUltrapassado, unidade, nivel.getAcaoRecomendada());
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

    public NivelAlerta getNivel() {
        return nivel;
    }

    public double getValorPrevisto() {
        return valorPrevisto;
    }

    public double getLimiteUltrapassado() {
        return limiteUltrapassado;
    }

    public String getEstrategia() {
        return estrategia;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    @Override
    public String toString() {
        return dataHora.format(FORMATO) + " " + explicar();
    }
}
