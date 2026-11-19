package br.com.fiap.territory.servico;

import br.com.fiap.territory.dominio.Alerta;
import br.com.fiap.territory.dominio.DadosTerritorio;
import br.com.fiap.territory.dominio.EstrategiaPrevisao;
import br.com.fiap.territory.dominio.Limiar;
import br.com.fiap.territory.dominio.Medicao;
import br.com.fiap.territory.dominio.NivelAlerta;
import br.com.fiap.territory.dominio.ResultadoPrevisao;
import br.com.fiap.territory.dominio.Sensor;
import br.com.fiap.territory.dominio.TipoEstrategia;
import br.com.fiap.territory.dominio.TipoSensor;
import br.com.fiap.territory.excecao.DadosInvalidosException;
import br.com.fiap.territory.excecao.FonteIndisponivelException;
import br.com.fiap.territory.persistencia.AlertaRepository;
import br.com.fiap.territory.persistencia.MedicaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Regras de negócio do monitoramento: ler e gravar medições, prever, decidir alertas.
 * Não lê teclado nem imprime nada: por isso dá para testar com Mockito, sem banco e sem console.
 */
@Service
public class ServicoMonitoramento {

    private final MedicaoRepository medicaoRepository;
    private final AlertaRepository alertaRepository;
    private final FabricaPrevisao fabricaPrevisao;
    private final ConfiguracaoSistema configuracao;
    private final Clock relogio;

    /** Construtor usado pelo Spring (há dois construtores, então indicamos qual com @Autowired). */
    @Autowired
    public ServicoMonitoramento(MedicaoRepository medicaoRepository, AlertaRepository alertaRepository,
                                FabricaPrevisao fabricaPrevisao, ConfiguracaoSistema configuracao) {
        this(medicaoRepository, alertaRepository, fabricaPrevisao, configuracao, Clock.systemDefaultZone());
    }

    /** Construtor com relógio controlável (útil em testes e na simulação de 24 horas). */
    ServicoMonitoramento(MedicaoRepository medicaoRepository, AlertaRepository alertaRepository,
                         FabricaPrevisao fabricaPrevisao, ConfiguracaoSistema configuracao, Clock relogio) {
        this.medicaoRepository = Objects.requireNonNull(medicaoRepository);
        this.alertaRepository = Objects.requireNonNull(alertaRepository);
        this.fabricaPrevisao = Objects.requireNonNull(fabricaPrevisao);
        this.configuracao = Objects.requireNonNull(configuracao);
        this.relogio = Objects.requireNonNull(relogio);
    }

    // ---------------------------------------------------------------- leituras

    /**
     * Lê o sensor, valida e grava a medição.
     *
     * @throws FonteIndisponivelException se o sensor não responder
     * @throws DadosInvalidosException    se o valor lido for fisicamente impossível (não é gravado)
     */
    public Medicao registrarLeitura(Sensor sensor) throws FonteIndisponivelException, DadosInvalidosException {
        if (sensor == null) {
            throw new FonteIndisponivelException("nenhum sensor informado para leitura");
        }
        double valor = sensor.lerValor();
        Medicao medicao = new Medicao(sensor.getCodigo(), sensor.getTipo(), valor, LocalDateTime.now(relogio));
        return medicaoRepository.save(medicao);
    }

    public List<Medicao> buscarHistoricoPorSensor(String codigoSensor) {
        return medicaoRepository.findByCodigoSensorOrderByDataHoraAscIdAsc(codigoSensor);
    }

    public long contarMedicoes() {
        return medicaoRepository.count();
    }

    // ---------------------------------------------------------------- previsões e alertas

    /** Roda a estratégia escolhida sobre o histórico do banco e emite alerta se preciso. */
    public AnalisePrevisao analisar(Sensor sensor, TipoEstrategia tipoEstrategia) {
        EstrategiaPrevisao estrategia = fabricaPrevisao.criar(tipoEstrategia);
        ResultadoPrevisao resultado = estrategia.prever(buscarHistoricoPorSensor(sensor.getCodigo()));
        Alerta alerta = avaliarAlerta(sensor.getCodigo(), sensor.getTipo(), resultado).orElse(null);
        return new AnalisePrevisao(sensor.getCodigo(), resultado, alerta);
    }

    /** Roda TODAS as estratégias para o sensor (polimorfismo: o laço não sabe qual é qual). */
    public List<AnalisePrevisao> analisarComTodasEstrategias(Sensor sensor) {
        List<Medicao> historico = buscarHistoricoPorSensor(sensor.getCodigo());
        return fabricaPrevisao.criarTodas().stream()
                .map(estrategia -> {
                    ResultadoPrevisao resultado = estrategia.prever(historico);
                    Alerta alerta = avaliarAlerta(sensor.getCodigo(), sensor.getTipo(), resultado).orElse(null);
                    return new AnalisePrevisao(sensor.getCodigo(), resultado, alerta);
                })
                .toList();
    }

    /**
     * Compara a previsão com os limiares do ConfiguracaoSistema.
     * Previsão não confiável (histórico curto) NUNCA gera alerta: o sistema se cala.
     * Se gerar, o alerta é gravado no banco.
     */
    public Optional<Alerta> avaliarAlerta(String codigoSensor, TipoSensor tipo, ResultadoPrevisao resultado) {
        if (resultado == null || !resultado.confiavel()) {
            return Optional.empty();
        }
        Limiar limiar = configuracao.getLimiar(tipo);
        Optional<NivelAlerta> nivel = limiar.classificar(resultado.valorPrevisto());
        if (nivel.isEmpty()) {
            return Optional.empty();
        }
        Alerta alerta = new Alerta(codigoSensor, tipo, nivel.get(), resultado.valorPrevisto(),
                limiar.limiteDo(nivel.get()), resultado.estrategia(), LocalDateTime.now(relogio));
        return Optional.of(alertaRepository.save(alerta));
    }

    public List<Alerta> listarAlertas() {
        return alertaRepository.findAllByOrderByDataHoraDesc();
    }

    public long contarAlertas() {
        return alertaRepository.count();
    }

    // ---------------------------------------------------------------- dados do território

    /**
     * Altera um limiar somente se o código de acesso do território estiver correto.
     *
     * @throws DadosInvalidosException código errado ou limiar inválido
     */
    public void alterarLimiar(DadosTerritorio territorio, String codigoAcesso, TipoSensor tipo, Limiar novoLimiar)
            throws DadosInvalidosException {
        territorio.validarCodigoAcesso(codigoAcesso);
        configuracao.definirLimiar(tipo, novoLimiar);
    }

    public ConfiguracaoSistema getConfiguracao() {
        return configuracao;
    }
}
