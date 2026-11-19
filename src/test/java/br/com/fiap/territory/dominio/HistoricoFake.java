package br.com.fiap.territory.dominio;

import br.com.fiap.territory.excecao.DadosInvalidosException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Ajuda dos testes: monta um histórico em ordem cronológica a partir de valores. */
public final class HistoricoFake {

    private HistoricoFake() {
    }

    public static List<Medicao> de(String codigoSensor, TipoSensor tipo, double... valores) {
        List<Medicao> historico = new ArrayList<>();
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 19, 0, 0);
        try {
            for (int i = 0; i < valores.length; i++) {
                historico.add(new Medicao(codigoSensor, tipo, valores[i], inicio.plusHours(i)));
            }
        } catch (DadosInvalidosException e) {
            throw new IllegalArgumentException("valor de teste inválido: " + e.getMessage(), e);
        }
        return historico;
    }
}
