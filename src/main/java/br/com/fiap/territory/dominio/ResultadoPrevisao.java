package br.com.fiap.territory.dominio;

/**
 * Resultado de uma previsão: o número E o que ele significa.
 *
 * @param estrategia     nome da estratégia que gerou a previsão
 * @param valorPrevisto  valor previsto (NaN quando não confiável)
 * @param leiturasUsadas quantas leituras do histórico existiam
 * @param confiavel      false quando o histórico é curto demais: o sistema "se cala"
 * @param significado    explicação em linguagem simples
 */
public record ResultadoPrevisao(String estrategia, double valorPrevisto, int leiturasUsadas,
                                boolean confiavel, String significado) {

    public static ResultadoPrevisao insuficiente(String estrategia, int leiturasDisponiveis, int minimoNecessario) {
        return new ResultadoPrevisao(estrategia, Double.NaN, leiturasDisponiveis, false, String.format(
                "Histórico insuficiente (%d de %d leituras): previsão não emitida para não gerar alarme falso.",
                leiturasDisponiveis, minimoNecessario));
    }

    /** Cópia deste resultado com outra explicação (usado pelas estratégias filhas). */
    public ResultadoPrevisao comSignificado(String novoSignificado) {
        return new ResultadoPrevisao(estrategia, valorPrevisto, leiturasUsadas, confiavel, novoSignificado);
    }
}
