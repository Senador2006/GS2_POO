package br.com.fiap.territory.dominio;

import java.util.List;

/**
 * Compara o período recente (últimas N leituras) com o período anterior (N leituras antes dele)
 * e projeta que a variação se repete: previsto = recente + (recente - anterior).
 * Responde mais rápido a tendências de subida do que a média móvel.
 */
public class PrevisaoComparacao extends EstrategiaPrevisao {

    private final int janela;

    public PrevisaoComparacao(int janela) {
        super("comparação com o período anterior", janela * 2);
        this.janela = janela;
    }

    @Override
    public ResultadoPrevisao prever(List<Medicao> historico) {
        ResultadoPrevisao base = super.prever(historico);
        if (!base.confiavel()) {
            return base;
        }
        List<Double> valores = historico.stream().map(Medicao::getValor).toList();
        double recente = mediaRecente(valores);
        double anterior = mediaAnterior(valores);
        String tendencia = recente > anterior ? "alta" : recente < anterior ? "queda" : "estabilidade";
        String variacao = anterior == 0
                ? "sem base de comparação (período anterior zerado)"
                : String.format("%+.1f%% em relação ao período anterior", (recente - anterior) / anterior * 100);
        return base.comSignificado(String.format(
                "Tendência de %s (%s). Mantido o ritmo, a próxima leitura deve chegar a %.2f %s.",
                tendencia, variacao, base.valorPrevisto(), unidadeDe(historico)));
    }

    @Override
    protected double calcularValorPrevisto(List<Double> valores) {
        double recente = mediaRecente(valores);
        double anterior = mediaAnterior(valores);
        return Math.max(0, recente + (recente - anterior));
    }

    private double mediaRecente(List<Double> valores) {
        return media(valores, valores.size() - janela, valores.size());
    }

    private double mediaAnterior(List<Double> valores) {
        return media(valores, valores.size() - 2 * janela, valores.size() - janela);
    }

    public int getJanela() {
        return janela;
    }
}
