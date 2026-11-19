package br.com.fiap.territory.dominio;

import br.com.fiap.territory.excecao.DadosInvalidosException;

/**
 * Qualquer "pedaço" do território que é monitorado (ponto vulnerável, equipamento público...).
 * Classe abstrata: não existe um componente "genérico"; cada filho decide
 * como calcular o próprio status em {@link #verificarStatus()}.
 */
public abstract class ComponenteTerritorial {

    private final String id;
    private String nome;
    private String regiao;
    private StatusComponente status = StatusComponente.NAO_VERIFICADO;

    protected ComponenteTerritorial(String id, String nome, String regiao) throws DadosInvalidosException {
        if (id == null || id.isBlank()) {
            throw new DadosInvalidosException("componente territorial sem identificador");
        }
        this.id = id;
        setNome(nome);
        setRegiao(regiao);
    }

    /**
     * Verifica a situação atual do componente, atualiza {@link #getStatus()} e devolve o resultado.
     */
    public abstract StatusComponente verificarStatus();

    /** Só os filhos podem mudar o status, e apenas por meio de verificarStatus(). */
    protected void atualizarStatus(StatusComponente novoStatus) {
        this.status = novoStatus;
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) throws DadosInvalidosException {
        if (nome == null || nome.isBlank()) {
            throw new DadosInvalidosException("o componente " + id + " precisa de um nome");
        }
        this.nome = nome.trim();
    }

    public String getRegiao() {
        return regiao;
    }

    public void setRegiao(String regiao) throws DadosInvalidosException {
        if (regiao == null || regiao.isBlank()) {
            throw new DadosInvalidosException("o componente " + id + " precisa de uma região");
        }
        this.regiao = regiao.trim();
    }

    public StatusComponente getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return String.format("%s - %s (%s) | status: %s", id, nome, regiao, status.getDescricao());
    }
}
