package br.com.fiap.territory.excecao;

/**
 * Lançada quando um dado do território não pode ser aceito:
 * medição fora da faixa física do sensor, população negativa,
 * limiar inválido, código de acesso incorreto etc.
 *
 * É checked (extends Exception) de propósito: quem chama é obrigado
 * a tratar ou repassar o erro, então ele nunca é "engolido" sem querer.
 */
public class DadosInvalidosException extends Exception {

    public DadosInvalidosException(String mensagem) {
        super(mensagem);
    }
}
