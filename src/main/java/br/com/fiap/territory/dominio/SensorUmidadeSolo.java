package br.com.fiap.territory.dominio;

import java.util.Random;

/** Umidade do solo em encostas (%): solo encharcado indica risco de deslizamento. */
public class SensorUmidadeSolo extends SensorSimulado {

    public SensorUmidadeSolo(String codigo) {
        super(codigo);
    }

    public SensorUmidadeSolo(String codigo, Random random, double probabilidadeFalha) {
        super(codigo, random, probabilidadeFalha);
    }

    @Override
    public TipoSensor getTipo() {
        return TipoSensor.UMIDADE_SOLO;
    }

    @Override
    protected double gerarValor(Random random) {
        return 30 + random.nextDouble() * 70;       // 30 a 100 %
    }
}
