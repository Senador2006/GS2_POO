package br.com.fiap.territory.dominio;

import java.util.List;
import java.util.Objects;

/**
 * Base das estratégias de previsão.
 *
 * O {@link #prever(List)} daqui faz a parte comum a todas: verifica se há leituras
 * suficientes (senão o sistema "se cala") e chama {@link #calcularValorPrevisto(List)},
 * que cada filha implementa do seu jeito. As filhas sobrescrevem prever(), chamam
 * super.prever() e acrescentam a explicação do resultado.
 */
public abstract class EstrategiaPrevisao {

    private final String nome;
    private final int minimoLeituras;

    protected EstrategiaPrevisao(String nome, int minimoLeituras) {
        if (minimoLeituras < 1) {
            throw new IllegalArgumentException("a estratégia precisa de pelo menos 1 leitura");
        }
        this.nome = nome;
        this.minimoLeituras = minimoLeituras;
    }

    /**
     * @param historico medições do sensor em ordem cronológica (mais antiga primeiro)
     */
    public ResultadoPrevisao prever(List<Medicao> historico) {
        Objects.requireNonNull(historico, "historico");
        if (historico.size() < minimoLeituras) {
            return ResultadoPrevisao.insuficiente(nome, historico.size(), minimoLeituras);
        }
        List<Double> valores = historico.stream().map(Medicao::getValor).toList();
        double previsto = calcularValorPrevisto(valores);
        return new ResultadoPrevisao(nome, previsto, historico.size(), true,
                String.format("Valor previsto: %.2f", previsto));
    }

    /** Cálculo específico de cada estratégia. Recebe pelo menos {@link #getMinimoLeituras()} valores. */
    protected abstract double calcularValorPrevisto(List<Double> valores);

    /** Média dos valores entre os índices [inicio, fim). Ajuda comum às filhas. */
    protected static double media(List<Double> valores, int inicio, int fim) {
        double soma = 0;
        for (int i = inicio; i < fim; i++) {
            soma += valores.get(i);
        }
        return soma / (fim - inicio);
    }

    /** Unidade da medição mais recente (para montar textos). */
    protected static String unidadeDe(List<Medicao> historico) {
        return historico.get(historico.size() - 1).getTipoSensor().getUnidade();
    }

    public String getNome() {
        return nome;
    }

    public int getMinimoLeituras() {
        return minimoLeituras;
    }
}
