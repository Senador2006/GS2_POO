package br.com.fiap.territory.dominio;

import br.com.fiap.territory.excecao.FonteIndisponivelException;

import java.util.Objects;
import java.util.Random;

/**
 * Base comum dos sensores simulados com {@link Random}.
 * Concentra o que é igual em todos (código, falha de comunicação, leitura corrompida)
 * e deixa para cada filho apenas COMO gerar um valor realista ({@link #gerarValor(Random)}).
 */
public abstract class SensorSimulado implements Sensor {

    /** Chance de o sensor não responder em uma leitura (gera FonteIndisponivelException). */
    private static final double PROBABILIDADE_FALHA_PADRAO = 0.05;

    /** Chance de a leitura vir corrompida (valor negativo, que a Medicao rejeita). */
    private static final double PROBABILIDADE_LEITURA_CORROMPIDA = 0.03;

    private final String codigo;
    private final Random random;
    private final double probabilidadeFalha;
    private boolean funcionando = true;

    protected SensorSimulado(String codigo, Random random, double probabilidadeFalha) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("o sensor precisa de um código");
        }
        this.codigo = codigo;
        this.random = Objects.requireNonNull(random, "random");
        this.probabilidadeFalha = probabilidadeFalha;
    }

    protected SensorSimulado(String codigo) {
        this(codigo, new Random(), PROBABILIDADE_FALHA_PADRAO);
    }

    @Override
    public double lerValor() throws FonteIndisponivelException {
        if (random.nextDouble() < probabilidadeFalha) {
            funcionando = false;
            throw new FonteIndisponivelException(String.format(
                    "sensor %s (%s) não respondeu à leitura", codigo, getTipo().getDescricao()));
        }
        funcionando = true;
        if (random.nextDouble() < PROBABILIDADE_LEITURA_CORROMPIDA) {
            // Simula um defeito de hardware: valor impossível que deve ser barrado na validação.
            return -1 - random.nextDouble() * 10;
        }
        return gerarValor(random);
    }

    /** Gera um valor realista dentro da faixa física do tipo de sensor. */
    protected abstract double gerarValor(Random random);

    @Override
    public String getCodigo() {
        return codigo;
    }

    @Override
    public boolean estaFuncionando() {
        return funcionando;
    }

    @Override
    public String toString() {
        return String.format("%s [%s] - %s", codigo, getTipo().getDescricao(),
                funcionando ? "funcionando" : "sem resposta");
    }
}
