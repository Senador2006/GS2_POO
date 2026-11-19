package br.com.fiap.territory.dominio;

import br.com.fiap.territory.excecao.FonteIndisponivelException;

/**
 * Contrato de qualquer sensor do território.
 * O resto do sistema só conhece esta interface, nunca a classe concreta
 * (polimorfismo): trocar um sensor simulado por um real não muda o serviço.
 */
public interface Sensor {

    /** Código único do sensor (ex.: "PLU-01"). É ele que liga a medição ao sensor no banco. */
    String getCodigo();

    /** Tipo do sensor (define unidade e faixa válida). */
    TipoSensor getTipo();

    /**
     * Faz uma leitura.
     *
     * @throws FonteIndisponivelException se o sensor não responder
     */
    double lerValor() throws FonteIndisponivelException;

    /** Indica se o sensor respondeu na última tentativa de leitura. */
    boolean estaFuncionando();
}
