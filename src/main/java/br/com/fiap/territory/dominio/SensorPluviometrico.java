package br.com.fiap.territory.dominio;

import java.util.Random;

/**
 * Pluviômetro: intensidade de chuva em mm/h.
 *
 * A chuva é simulada em EPISÓDIOS: uma pancada forte costuma durar algumas leituras seguidas,
 * em vez de cada leitura ser sorteada sem relação com a anterior. Assim as previsões têm
 * tendências reais para detectar (subida, pico, fim da chuva).
 */
public class SensorPluviometrico extends SensorSimulado {

    /** Chance de começar uma pancada forte numa leitura em que não estava chovendo forte. */
    private static final double PROBABILIDADE_INICIO_EPISODIO = 0.15;

    /** Chance de a pancada acabar a cada leitura (em média dura cerca de 3 leituras). */
    private static final double PROBABILIDADE_FIM_EPISODIO = 0.30;

    static final double LIMITE_CHUVA_FRACA = 15;
    static final double LIMITE_CHUVA_FORTE = 85;

    private boolean emEpisodio;
    private double intensidadeEpisodio;

    public SensorPluviometrico(String codigo) {
        super(codigo);
    }

    public SensorPluviometrico(String codigo, Random random, double probabilidadeFalha) {
        super(codigo, random, probabilidadeFalha);
    }

    @Override
    public TipoSensor getTipo() {
        return TipoSensor.PLUVIOMETRO;
    }

    @Override
    protected double gerarValor(Random random) {
        if (emEpisodio && random.nextDouble() < PROBABILIDADE_FIM_EPISODIO) {
            emEpisodio = false;
        } else if (!emEpisodio && random.nextDouble() < PROBABILIDADE_INICIO_EPISODIO) {
            emEpisodio = true;
            intensidadeEpisodio = LIMITE_CHUVA_FRACA + random.nextDouble() * 25;   // começa entre 15 e 40 mm/h
        }
        if (!emEpisodio) {
            return random.nextDouble() * LIMITE_CHUVA_FRACA;                        // 0 a 15 mm/h
        }
        // Durante o episódio a intensidade varia em torno da anterior, com leve tendência de subir.
        intensidadeEpisodio += random.nextDouble() * 20 - 7;
        intensidadeEpisodio = Math.max(LIMITE_CHUVA_FRACA, Math.min(LIMITE_CHUVA_FORTE, intensidadeEpisodio));
        return intensidadeEpisodio;                                                 // 15 a 85 mm/h
    }
}
