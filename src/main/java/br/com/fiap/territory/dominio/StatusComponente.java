package br.com.fiap.territory.dominio;

/** Situação operacional de um componente do território. */
public enum StatusComponente {

    OPERANDO("Operando: todos os sensores responderam"),
    PARCIAL("Parcial: parte dos sensores não respondeu"),
    INOPERANTE("Inoperante: nenhum sensor respondeu"),
    NAO_VERIFICADO("Ainda não verificado");

    private final String descricao;

    StatusComponente(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
