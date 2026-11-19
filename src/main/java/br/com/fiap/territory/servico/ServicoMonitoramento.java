package br.com.fiap.territory.servico;

import br.com.fiap.territory.dominio.Alerta;
import br.com.fiap.territory.dominio.DadosTerritorio;
import br.com.fiap.territory.dominio.EstrategiaPrevisao;
import br.com.fiap.territory.dominio.Limiar;
import br.com.fiap.territory.dominio.LimiarSalvo;
import br.com.fiap.territory.dominio.Medicao;
import br.com.fiap.territory.dominio.NivelAlerta;
import br.com.fiap.territory.dominio.ResultadoPrevisao;
import br.com.fiap.territory.dominio.Sensor;
import br.com.fiap.territory.dominio.TipoEstrategia;
import br.com.fiap.territory.dominio.TipoSensor;
import br.com.fiap.territory.excecao.DadosInvalidosException;
import br.com.fiap.territory.excecao.FonteIndisponivelException;
import br.com.fiap.territory.persistencia.AlertaRepository;
import br.com.fiap.territory.persistencia.LimiarRepository;
import br.com.fiap.territory.persistencia.MedicaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
    private final LimiarRepository limiarRepository;
    private final FabricaPrevisao fabricaPrevisao;
    private final ConfiguracaoSistema configuracao;
    private final Clock relogio;

    /** Construtor usado pelo Spring (há dois construtores, então indicamos qual com @Autowired). */
    @Autowired
    public ServicoMonitoramento(MedicaoRepository medicaoRepository, AlertaRepository alertaRepository,
                                LimiarRepository limiarRepository, FabricaPrevisao fabricaPrevisao,
                                ConfiguracaoSistema configuracao) {
        this(medicaoRepository, alertaRepository, limiarRepository, fabricaPrevisao, configuracao,
                Clock.systemDefaultZone());
    }

    /** Construtor com relógio controlável (útil em testes e na simulação de 24 horas). */
    ServicoMonitoramento(MedicaoRepository medicaoRepository, AlertaRepository alertaRepository,
                         LimiarRepository limiarRepository, FabricaPrevisao fabricaPrevisao,
                         ConfiguracaoSistema configuracao, Clock relogio) {
        this.medicaoRepository = Objects.requireNonNull(medicaoRepository);
        this.alertaRepository = Objects.requireNonNull(alertaRepository);
        this.limiarRepository = Objects.requireNonNull(limiarRepository);
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
        List<Medicao> historico = buscarHistoricoPorSensor(sensor.getCodigo());
        return analisarComEstrategia(sensor, estrategia, historico);
    }

    /** Roda TODAS as estratégias para o sensor (polimorfismo: o laço não sabe qual é qual). */
    public List<AnalisePrevisao> analisarComTodasEstrategias(Sensor sensor) {
        List<Medicao> historico = buscarHistoricoPorSensor(sensor.getCodigo());
        return fabricaPrevisao.criarTodas().stream()
                .map(estrategia -> analisarComEstrategia(sensor, estrategia, historico))
                .toList();
    }

    private AnalisePrevisao analisarComEstrategia(Sensor sensor, EstrategiaPrevisao estrategia,
                                                  List<Medicao> historico) {
        ResultadoPrevisao resultado = estrategia.prever(historico);
        Optional<Alerta> repetido = buscarAlertaRepetido(sensor, resultado, historico);
        if (repetido.isPresent()) {
            return new AnalisePrevisao(sensor.getCodigo(), resultado, repetido.get(), true);
        }
        Alerta alerta = avaliarAlerta(sensor.getCodigo(), sensor.getTipo(), resultado).orElse(null);
        return new AnalisePrevisao(sensor.getCodigo(), resultado, alerta, false);
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

    /**
     * Rodar a previsão de novo sem leituras novas daria o mesmo resultado. Para não encher a tabela
     * ALERTA de cópias, devolve o último alerta do sensor/estratégia quando ele tem o MESMO nível
     * e nenhuma medição chegou depois dele.
     */
    private Optional<Alerta> buscarAlertaRepetido(Sensor sensor, ResultadoPrevisao resultado,
                                                  List<Medicao> historico) {
        if (!resultado.confiavel() || historico.isEmpty()) {
            return Optional.empty();
        }
        Optional<NivelAlerta> nivel = configuracao.getLimiar(sensor.getTipo()).classificar(resultado.valorPrevisto());
        if (nivel.isEmpty()) {
            return Optional.empty();
        }
        LocalDateTime ultimaLeitura = historico.get(historico.size() - 1).getDataHora();
        return alertaRepository
                .findFirstByCodigoSensorAndEstrategiaOrderByDataHoraDescIdDesc(sensor.getCodigo(),
                        resultado.estrategia())
                .filter(anterior -> anterior.getNivel() == nivel.get())
                .filter(anterior -> !ultimaLeitura.isAfter(anterior.getDataHora()));
    }

    public List<Alerta> listarAlertas() {
        return alertaRepository.findAllByOrderByDataHoraDesc();
    }

    public long contarAlertas() {
        return alertaRepository.count();
    }

    // ---------------------------------------------------------------- dados do território

    /**
     * Altera um limiar somente se o código de acesso do território estiver correto
     * e grava a alteração no banco, para valer também depois de reiniciar.
     *
     * @throws DadosInvalidosException código errado ou limiar inválido
     */
    public void alterarLimiar(DadosTerritorio territorio, String codigoAcesso, TipoSensor tipo, Limiar novoLimiar)
            throws DadosInvalidosException {
        territorio.validarCodigoAcesso(codigoAcesso);
        configuracao.definirLimiar(tipo, novoLimiar);
        limiarRepository.save(new LimiarSalvo(tipo, novoLimiar, LocalDateTime.now(relogio)));
    }

    /**
     * Apaga os limiares gravados e volta aos valores padrão (exige o código de acesso).
     *
     * @throws DadosInvalidosException código errado
     */
    public void restaurarLimiaresPadrao(DadosTerritorio territorio, String codigoAcesso)
            throws DadosInvalidosException {
        territorio.validarCodigoAcesso(codigoAcesso);
        limiarRepository.deleteAll();
        configuracao.restaurarPadroes();
    }

    /**
     * Aplica no ConfiguracaoSistema os limiares gravados em execuções anteriores.
     * Se algum estiver inválido no banco, nenhum é aplicado (todos seguem no padrão).
     *
     * @return quantos tipos de sensor passaram a usar um limiar personalizado
     * @throws DadosInvalidosException se alguma linha da tabela LIMIAR tiver valores inválidos
     */
    public int carregarLimiaresSalvos() throws DadosInvalidosException {
        Map<TipoSensor, Limiar> salvos = new EnumMap<>(TipoSensor.class);
        for (LimiarSalvo salvo : limiarRepository.findAll()) {
            salvos.put(salvo.getTipoSensor(), salvo.paraLimiar());
        }
        for (Map.Entry<TipoSensor, Limiar> salvo : salvos.entrySet()) {
            configuracao.definirLimiar(salvo.getKey(), salvo.getValue());
        }
        return salvos.size();
    }

    public ConfiguracaoSistema getConfiguracao() {
        return configuracao;
    }
}
