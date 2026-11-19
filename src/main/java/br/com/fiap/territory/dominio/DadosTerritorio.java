package br.com.fiap.territory.dominio;

import br.com.fiap.territory.excecao.DadosInvalidosException;

/**
 * Dados gerais do território monitorado. Todos os atributos são privados e
 * só mudam por setters que validam (encapsulamento).
 *
 * O código de acesso nunca sai do objeto (não há getter): ele só é usado para
 * autorizar a alteração de limiares em {@link #validarCodigoAcesso(String)}.
 *
 * Sem dados pessoais: o território é descrito por região e números agregados.
 */
public class DadosTerritorio {

    private String nomeTerritorio;
    private int populacaoEstimada;
    private int domiciliosEmAreaDeRisco;
    private final String codigoAcesso;

    public DadosTerritorio(String nomeTerritorio, int populacaoEstimada, int domiciliosEmAreaDeRisco,
                           String codigoAcesso) throws DadosInvalidosException {
        if (codigoAcesso == null || codigoAcesso.isBlank()) {
            throw new DadosInvalidosException("o território precisa de um código de acesso para proteger os limiares");
        }
        this.codigoAcesso = codigoAcesso;
        setNomeTerritorio(nomeTerritorio);
        setPopulacaoEstimada(populacaoEstimada);
        setDomiciliosEmAreaDeRisco(domiciliosEmAreaDeRisco);
    }

    /**
     * Confere o código antes de qualquer alteração de limiar.
     *
     * @throws DadosInvalidosException se o código estiver errado
     */
    public void validarCodigoAcesso(String codigoInformado) throws DadosInvalidosException {
        if (!codigoAcesso.equals(codigoInformado)) {
            throw new DadosInvalidosException("código de acesso incorreto: os limiares não foram alterados");
        }
    }

    public String getNomeTerritorio() {
        return nomeTerritorio;
    }

    public void setNomeTerritorio(String nomeTerritorio) throws DadosInvalidosException {
        if (nomeTerritorio == null || nomeTerritorio.isBlank()) {
            throw new DadosInvalidosException("o nome do território não pode ficar vazio");
        }
        this.nomeTerritorio = nomeTerritorio.trim();
    }

    public int getPopulacaoEstimada() {
        return populacaoEstimada;
    }

    public void setPopulacaoEstimada(int populacaoEstimada) throws DadosInvalidosException {
        if (populacaoEstimada < 0) {
            throw new DadosInvalidosException("população negativa não é válida: " + populacaoEstimada);
        }
        this.populacaoEstimada = populacaoEstimada;
    }

    public int getDomiciliosEmAreaDeRisco() {
        return domiciliosEmAreaDeRisco;
    }

    public void setDomiciliosEmAreaDeRisco(int domiciliosEmAreaDeRisco) throws DadosInvalidosException {
        if (domiciliosEmAreaDeRisco < 0) {
            throw new DadosInvalidosException("quantidade de domicílios em área de risco negativa: " + domiciliosEmAreaDeRisco);
        }
        this.domiciliosEmAreaDeRisco = domiciliosEmAreaDeRisco;
    }

    @Override
    public String toString() {
        return String.format("%s | população estimada: %d | domicílios em área de risco: %d",
                nomeTerritorio, populacaoEstimada, domiciliosEmAreaDeRisco);
    }
}
