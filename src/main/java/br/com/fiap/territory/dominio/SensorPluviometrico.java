package br.com.fiap.territory.dominio;

import java.util.Random;

/** Pluviômetro: intensidade de chuva em mm/h. */
public class SensorPluviometrico extends SensorSimulado {

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
        // Na maior parte do tempo chove pouco; às vezes vem uma pancada forte.
        if (random.nextDouble() < 0.7) {
            return random.nextDouble() * 15;        // 0 a 15 mm/h
        }
        return 15 + random.nextDouble() * 70;       // 15 a 85 mm/h
    }
}
