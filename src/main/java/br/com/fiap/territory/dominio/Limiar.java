package br.com.fiap.territory.dominio;

import br.com.fiap.territory.excecao.DadosInvalidosException;

import java.util.Optional;

/**
 * Três valores de corte de um tipo de sensor: ATENÇÃO &lt; ALERTA &lt; CRÍTICO.
 * Imutável: para mudar um limiar, cria-se um novo objeto (já validado).
 */
public final class Limiar {

    private final double atencao;
    private final double alerta;
    private final double critico;

    public Limiar(double atencao, double alerta, double critico) throws DadosInvalidosException {
        if (atencao < 0 || alerta < 0 || critico < 0) {
            throw new DadosInvalidosException("limiar inválido: valores não podem ser negativos");
        }
        if (!(atencao < alerta && alerta < critico)) {
            throw new DadosInvalidosException(String.format(
                    "limiar inválido: é preciso atenção < alerta < crítico (recebido %.2f / %.2f / %.2f)",
                    atencao, alerta, critico));
        }
        this.atencao = atencao;
        this.alerta = alerta;
        this.critico = critico;
    }

    /**
     * Classifica um valor previsto. Devolve vazio quando o valor está abaixo de todos os limiares
     * (nenhum alerta deve ser emitido).
     */
    public Optional<NivelAlerta> classificar(double valor) {
        if (valor >= critico) {
            return Optional.of(NivelAlerta.CRITICO);
        }
        if (valor >= alerta) {
            return Optional.of(NivelAlerta.ALERTA);
        }
        if (valor >= atencao) {
            return Optional.of(NivelAlerta.ATENCAO);
        }
        return Optional.empty();
    }

    /** Valor de corte correspondente a um nível (usado para explicar qual limite estourou). */
    public double limiteDo(NivelAlerta nivel) {
        return switch (nivel) {
            case ATENCAO -> atencao;
            case ALERTA -> alerta;
            case CRITICO -> critico;
        };
    }

    public double getAtencao() {
        return atencao;
    }

    public double getAlerta() {
        return alerta;
    }

    public double getCritico() {
        return critico;
    }

    @Override
    public String toString() {
        return String.format("atenção >= %.1f | alerta >= %.1f | crítico >= %.1f", atencao, alerta, critico);
    }
}
