package br.com.fiap.territory.dominio;

/** Estratégias de previsão disponíveis. A FabricaPrevisao transforma cada uma em objeto. */
public enum TipoEstrategia {

    MEDIA_MOVEL("Média móvel"),
    COMPARACAO("Comparação com o período anterior");

    private final String descricao;

    TipoEstrategia(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
