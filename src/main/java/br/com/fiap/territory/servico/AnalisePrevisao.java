package br.com.fiap.territory.servico;

import br.com.fiap.territory.dominio.Alerta;
import br.com.fiap.territory.dominio.ResultadoPrevisao;

import java.util.Optional;

/**
 * Resultado de rodar uma estratégia para um sensor: a previsão e, se houver, o alerta emitido.
 *
 * @param alerta null quando a previsão não estourou nenhum limiar (ou não era confiável)
 */
public record AnalisePrevisao(String codigoSensor, ResultadoPrevisao resultado, Alerta alerta) {

    public Optional<Alerta> alertaEmitido() {
        return Optional.ofNullable(alerta);
    }
}
