package br.com.fiap.territory.dominio;

/** Níveis de alerta, do mais leve ao mais grave, com a ação recomendada para cada um. */
public enum NivelAlerta {

    ATENCAO("ATENÇÃO", "Acompanhar as próximas leituras e avisar os moradores dos pontos vulneráveis."),
    ALERTA("ALERTA", "Preparar a equipe de apoio, liberar bueiros e indicar rotas seguras."),
    CRITICO("CRÍTICO", "Acionar a Defesa Civil e orientar a saída preventiva das áreas de risco.");

    private final String rotulo;
    private final String acaoRecomendada;

    NivelAlerta(String rotulo, String acaoRecomendada) {
        this.rotulo = rotulo;
        this.acaoRecomendada = acaoRecomendada;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getAcaoRecomendada() {
        return acaoRecomendada;
    }
}
