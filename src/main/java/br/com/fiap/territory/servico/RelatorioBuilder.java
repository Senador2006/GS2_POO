package br.com.fiap.territory.servico;

import br.com.fiap.territory.dominio.Alerta;
import br.com.fiap.territory.dominio.DadosTerritorio;
import br.com.fiap.territory.dominio.Limiar;
import br.com.fiap.territory.dominio.PontoMonitoramento;
import br.com.fiap.territory.dominio.TipoSensor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * BUILDER: monta o relatório de status seção por seção.
 *
 * Por que Builder aqui: o relatório tem várias partes opcionais (território, pontos, limiares,
 * totais, últimos alertas). Um construtor com todas elas como parâmetro seria ilegível e obrigaria
 * a passar null no que não se quer mostrar. Com o builder, quem chama escolhe as seções e a ordem,
 * e a formatação fica toda aqui, fora do menu.
 */
public class RelatorioBuilder {

    private final String titulo;
    private final List<String> secoes = new ArrayList<>();

    public RelatorioBuilder(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("o relatório precisa de um título");
        }
        this.titulo = titulo.trim();
    }

    public RelatorioBuilder comTerritorio(DadosTerritorio dados) {
        secoes.add("Território: " + Objects.requireNonNull(dados, "dados"));
        return this;
    }

    /** Usa o status atual de cada ponto: quem chama decide se roda verificarStatus() antes. */
    public RelatorioBuilder comPontos(List<PontoMonitoramento> pontos) {
        StringBuilder texto = new StringBuilder("Pontos monitorados:");
        for (PontoMonitoramento ponto : Objects.requireNonNull(pontos, "pontos")) {
            texto.append("\n  ").append(ponto);
            ponto.getSensores().forEach(s -> texto.append("\n     - ").append(s));
        }
        secoes.add(texto.toString());
        return this;
    }

    public RelatorioBuilder comLimiares(Map<TipoSensor, Limiar> limiares) {
        StringBuilder texto = new StringBuilder("Limiares em uso:");
        for (Map.Entry<TipoSensor, Limiar> e : Objects.requireNonNull(limiares, "limiares").entrySet()) {
            texto.append(String.format("%n  %-18s %s (%s)", e.getKey().getDescricao(), e.getValue(),
                    e.getKey().getUnidade()));
        }
        secoes.add(texto.toString());
        return this;
    }

    public RelatorioBuilder comTotaisDoBanco(long medicoes, long alertas) {
        secoes.add("Banco: " + medicoes + " medição(ões) e " + alertas + " alerta(s) gravados.");
        return this;
    }

    /** @param alertas lista do mais recente para o mais antigo; só os {@code quantidade} primeiros entram */
    public RelatorioBuilder comUltimosAlertas(List<Alerta> alertas, int quantidade) {
        Objects.requireNonNull(alertas, "alertas");
        if (alertas.isEmpty()) {
            secoes.add("Últimos alertas: nenhum alerta emitido até agora.");
            return this;
        }
        StringBuilder texto = new StringBuilder("Últimos alertas:");
        alertas.stream().limit(Math.max(quantidade, 0)).forEach(a -> texto.append("\n  ").append(a));
        secoes.add(texto.toString());
        return this;
    }

    /**
     * @throws IllegalStateException se nenhuma seção foi adicionada (relatório vazio não faz sentido)
     */
    public String construir() {
        if (secoes.isEmpty()) {
            throw new IllegalStateException("relatório \"" + titulo + "\" sem nenhuma seção");
        }
        return "--- " + titulo + " ---\n" + String.join("\n\n", secoes);
    }
}
