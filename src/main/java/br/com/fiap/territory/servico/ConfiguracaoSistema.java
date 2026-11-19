package br.com.fiap.territory.servico;

import br.com.fiap.territory.dominio.Limiar;
import br.com.fiap.territory.dominio.TipoEstrategia;
import br.com.fiap.territory.dominio.TipoSensor;
import br.com.fiap.territory.excecao.DadosInvalidosException;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * SINGLETON: única instância com os parâmetros do sistema (limiares por tipo de sensor,
 * tamanho das janelas de previsão e estratégia padrão).
 *
 * Por que Singleton aqui: os limiares precisam ser os MESMOS para o serviço de alertas,
 * para o menu que os exibe e para quem os altera. Uma única fonte evita que duas partes
 * do sistema comparem previsões com limiares diferentes.
 */
public final class ConfiguracaoSistema {

    private static final ConfiguracaoSistema INSTANCIA = new ConfiguracaoSistema();

    private final Map<TipoSensor, Limiar> limiares = new EnumMap<>(TipoSensor.class);
    private int janelaMediaMovel;
    private int janelaComparacao;
    private TipoEstrategia estrategiaPadrao;

    /** Construtor privado: ninguém fora daqui consegue dar "new ConfiguracaoSistema()". */
    private ConfiguracaoSistema() {
        restaurarPadroes();
    }

    public static ConfiguracaoSistema getInstancia() {
        return INSTANCIA;
    }

    /** Volta aos valores padrão. Útil no início e nos testes (que não podem depender uns dos outros). */
    public void restaurarPadroes() {
        limiares.put(TipoSensor.PLUVIOMETRO, limiarPadrao(20, 40, 60));       // mm/h
        limiares.put(TipoSensor.NIVEL_CORREGO, limiarPadrao(2.5, 3.5, 4.5));  // m
        limiares.put(TipoSensor.UMIDADE_SOLO, limiarPadrao(70, 85, 95));      // %
        janelaMediaMovel = 5;
        janelaComparacao = 3;
        estrategiaPadrao = TipoEstrategia.MEDIA_MOVEL;
    }

    private static Limiar limiarPadrao(double atencao, double alerta, double critico) {
        try {
            return new Limiar(atencao, alerta, critico);
        } catch (DadosInvalidosException e) {
            // Só acontece se alguém digitar um padrão errado no código: falha logo na inicialização.
            throw new IllegalStateException("limiar padrão mal configurado: " + e.getMessage(), e);
        }
    }

    public Limiar getLimiar(TipoSensor tipo) {
        return limiares.get(tipo);
    }

    public Map<TipoSensor, Limiar> getLimiares() {
        return Collections.unmodifiableMap(limiares);
    }

    /**
     * Troca o limiar de um tipo de sensor. Quem chama deve ter validado o código de acesso
     * (ver ServicoMonitoramento#alterarLimiar).
     */
    public void definirLimiar(TipoSensor tipo, Limiar limiar) throws DadosInvalidosException {
        if (tipo == null || limiar == null) {
            throw new DadosInvalidosException("tipo de sensor e limiar são obrigatórios");
        }
        limiares.put(tipo, limiar);
    }

    public int getJanelaMediaMovel() {
        return janelaMediaMovel;
    }

    public int getJanelaComparacao() {
        return janelaComparacao;
    }

    public TipoEstrategia getEstrategiaPadrao() {
        return estrategiaPadrao;
    }

    public void setEstrategiaPadrao(TipoEstrategia estrategiaPadrao) {
        this.estrategiaPadrao = Objects.requireNonNull(estrategiaPadrao);
    }
}
