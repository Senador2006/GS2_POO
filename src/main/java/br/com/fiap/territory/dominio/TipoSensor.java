package br.com.fiap.territory.dominio;

import br.com.fiap.territory.excecao.DadosInvalidosException;

/**
 * Tipos de sensor do recorte "Clima local: chuva intensa e risco de alagamento".
 * Cada tipo conhece sua unidade e a faixa FÍSICA aceitável de leitura.
 * A faixa física é diferente do limiar de alerta: fora dela a medição é
 * impossível (defeito), e não um risco.
 */
public enum TipoSensor {

    PLUVIOMETRO("Pluviômetro", "mm/h", 0.0, 150.0),
    NIVEL_CORREGO("Nível do córrego", "m", 0.0, 10.0),
    UMIDADE_SOLO("Umidade do solo", "%", 0.0, 100.0);

    private final String descricao;
    private final String unidade;
    private final double valorMinimo;
    private final double valorMaximo;

    TipoSensor(String descricao, String unidade, double valorMinimo, double valorMaximo) {
        this.descricao = descricao;
        this.unidade = unidade;
        this.valorMinimo = valorMinimo;
        this.valorMaximo = valorMaximo;
    }

    /**
     * Valida se o valor é fisicamente possível para este tipo de sensor.
     *
     * @throws DadosInvalidosException se o valor for NaN ou estiver fora da faixa
     */
    public void validar(double valor) throws DadosInvalidosException {
        if (Double.isNaN(valor) || valor < valorMinimo || valor > valorMaximo) {
            throw new DadosInvalidosException(String.format(
                    "medição de %s fora da faixa: %.2f %s (aceito de %.1f a %.1f %s)",
                    descricao.toLowerCase(), valor, unidade, valorMinimo, valorMaximo, unidade));
        }
    }

    public String getDescricao() {
        return descricao;
    }

    public String getUnidade() {
        return unidade;
    }

    public double getValorMinimo() {
        return valorMinimo;
    }

    public double getValorMaximo() {
        return valorMaximo;
    }
}
