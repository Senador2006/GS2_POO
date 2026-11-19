package br.com.fiap.territory.dominio;

import java.util.List;

/**
 * Prevê a próxima leitura como a média das últimas N leituras.
 * Boa para suavizar picos isolados: responde a situações que se mantêm.
 */
public class PrevisaoMediaMovel extends EstrategiaPrevisao {

    private final int janela;

    public PrevisaoMediaMovel(int janela) {
        super("média móvel de " + janela + " leituras", janela);
        this.janela = janela;
    }

    @Override
    public ResultadoPrevisao prever(List<Medicao> historico) {
        ResultadoPrevisao base = super.prever(historico);
        if (!base.confiavel()) {
            return base;
        }
        return base.comSignificado(String.format(
                "Se o padrão das últimas %d leituras continuar, a próxima deve ficar perto de %.2f %s.",
                janela, base.valorPrevisto(), unidadeDe(historico)));
    }

    @Override
    protected double calcularValorPrevisto(List<Double> valores) {
        return media(valores, valores.size() - janela, valores.size());
    }

    public int getJanela() {
        return janela;
    }
}
