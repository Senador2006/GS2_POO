package br.com.fiap.territory.servico;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Entrega ao Spring os objetos criados pelos nossos padrões (Singleton e Factory),
 * para que possam ser injetados no ServicoMonitoramento.
 * O Spring NÃO cria outra ConfiguracaoSistema: ele reaproveita getInstancia().
 */
@Configuration
public class BeansConfig {

    @Bean
    public ConfiguracaoSistema configuracaoSistema() {
        return ConfiguracaoSistema.getInstancia();
    }

    @Bean
    public FabricaPrevisao fabricaPrevisao(ConfiguracaoSistema configuracaoSistema) {
        return new FabricaPrevisao(configuracaoSistema);
    }
}
