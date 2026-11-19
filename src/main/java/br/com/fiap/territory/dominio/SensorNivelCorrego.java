package br.com.fiap.territory.dominio;

import java.util.Random;

/** Régua/sensor ultrassônico de nível do córrego, em metros. */
public class SensorNivelCorrego extends SensorSimulado {

    public SensorNivelCorrego(String codigo) {
        super(codigo);
    }

    public SensorNivelCorrego(String codigo, Random random, double probabilidadeFalha) {
        super(codigo, random, probabilidadeFalha);
    }

    @Override
    public TipoSensor getTipo() {
        return TipoSensor.NIVEL_CORREGO;
    }

    @Override
    protected double gerarValor(Random random) {
        return 0.5 + random.nextDouble() * 4.5;     // 0,5 a 5,0 m
    }
}
