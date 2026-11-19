package br.com.fiap.territory.servico;

import br.com.fiap.territory.dominio.Alerta;
import br.com.fiap.territory.dominio.ResultadoPrevisao;

import java.util.Optional;

/**
 * Resultado de rodar uma estratégia para um sensor: a previsão e, se houver, o alerta.
 *
 * @param alerta          null quando a previsão não estourou nenhum limiar (ou não era confiável)
 * @param alertaRepetido  true quando o alerta é o mesmo já emitido antes e não chegou nenhuma leitura nova
 *                        desde então: ele é mostrado de novo, mas NÃO é gravado outra vez
 */
public record AnalisePrevisao(String codigoSensor, ResultadoPrevisao resultado, Alerta alerta,
                              boolean alertaRepetido) {

    public Optional<Alerta> alertaEmitido() {
        return Optional.ofNullable(alerta);
    }
}
