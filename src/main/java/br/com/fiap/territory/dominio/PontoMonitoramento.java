package br.com.fiap.territory.dominio;

import br.com.fiap.territory.excecao.DadosInvalidosException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ponto vulnerável a alagamento/deslizamento (margem de córrego, encosta, passagem subterrânea)
 * com um conjunto de sensores instalados. É a classe concreta de {@link ComponenteTerritorial}.
 */
public class PontoMonitoramento extends ComponenteTerritorial {

    private final List<Sensor> sensores = new ArrayList<>();

    public PontoMonitoramento(String id, String nome, String regiao) throws DadosInvalidosException {
        super(id, nome, regiao);
    }

    public void adicionarSensor(Sensor sensor) throws DadosInvalidosException {
        if (sensor == null) {
            throw new DadosInvalidosException("não é possível instalar um sensor nulo no ponto " + getId());
        }
        sensores.add(sensor);
    }

    /** Lista somente leitura: ninguém de fora altera os sensores sem passar por adicionarSensor(). */
    public List<Sensor> getSensores() {
        return Collections.unmodifiableList(sensores);
    }

    @Override
    public StatusComponente verificarStatus() {
        long funcionando = sensores.stream().filter(Sensor::estaFuncionando).count();
        StatusComponente novo;
        if (sensores.isEmpty() || funcionando == 0) {
            novo = StatusComponente.INOPERANTE;
        } else if (funcionando == sensores.size()) {
            novo = StatusComponente.OPERANDO;
        } else {
            novo = StatusComponente.PARCIAL;
        }
        atualizarStatus(novo);
        return novo;
    }
}
