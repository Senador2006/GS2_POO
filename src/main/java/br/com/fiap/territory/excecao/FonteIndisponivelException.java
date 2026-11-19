package br.com.fiap.territory.excecao;

/**
 * Lançada quando a fonte de dados falha: o sensor não respondeu,
 * o gerador de dados não está disponível ou não há sensor com o código pedido.
 */
public class FonteIndisponivelException extends Exception {

    public FonteIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public FonteIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
