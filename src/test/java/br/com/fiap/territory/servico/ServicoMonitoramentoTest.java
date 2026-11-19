package br.com.fiap.territory.servico;

import br.com.fiap.territory.dominio.Alerta;
import br.com.fiap.territory.dominio.DadosTerritorio;
import br.com.fiap.territory.dominio.HistoricoFake;
import br.com.fiap.territory.dominio.Limiar;
import br.com.fiap.territory.dominio.LimiarSalvo;
import br.com.fiap.territory.dominio.Medicao;
import br.com.fiap.territory.dominio.NivelAlerta;
import br.com.fiap.territory.dominio.PrevisaoMediaMovel;
import br.com.fiap.territory.dominio.Sensor;
import br.com.fiap.territory.dominio.TipoEstrategia;
import br.com.fiap.territory.dominio.TipoSensor;
import br.com.fiap.territory.excecao.DadosInvalidosException;
import br.com.fiap.territory.excecao.FonteIndisponivelException;
import br.com.fiap.territory.persistencia.AlertaRepository;
import br.com.fiap.territory.persistencia.LimiarRepository;
import br.com.fiap.territory.persistencia.MedicaoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Testa a lógica de alerta SEM banco: os repositories são mocks do Mockito. */
@ExtendWith(MockitoExtension.class)
class ServicoMonitoramentoTest {

    private static final String MEDIA_MOVEL = new PrevisaoMediaMovel(5).getNome();

    @Mock
    private MedicaoRepository medicaoRepository;

    @Mock
    private AlertaRepository alertaRepository;

    @Mock
    private LimiarRepository limiarRepository;

    @Mock
    private Sensor sensor;

    private ServicoMonitoramento servico;

    @BeforeEach
    void prepararServico() {
        ConfiguracaoSistema configuracao = ConfiguracaoSistema.getInstancia();
        configuracao.restaurarPadroes(); // pluviômetro: 20 / 40 / 60 mm/h
        Clock relogioFixo = Clock.fixed(Instant.parse("2026-11-19T12:00:00Z"), ZoneId.of("UTC"));
        servico = new ServicoMonitoramento(medicaoRepository, alertaRepository, limiarRepository,
                new FabricaPrevisao(configuracao), configuracao, relogioFixo);
    }

    @AfterEach
    void desfazerLimiaresAlterados() {
        ConfiguracaoSistema.getInstancia().restaurarPadroes();
    }

    @Test
    void deveEmitirAlertaCriticoQuandoMediaMovelPassaDoLimite() {
        // Arrange: média das últimas 5 = 70 mm/h (crítico >= 60)
        when(sensor.getCodigo()).thenReturn("PLU-01");
        when(sensor.getTipo()).thenReturn(TipoSensor.PLUVIOMETRO);
        when(medicaoRepository.findByCodigoSensorOrderByDataHoraAscIdAsc("PLU-01"))
                .thenReturn(HistoricoFake.de("PLU-01", TipoSensor.PLUVIOMETRO, 60, 65, 70, 75, 80));
        when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        AnalisePrevisao analise = servico.analisar(sensor, TipoEstrategia.MEDIA_MOVEL);

        // Assert
        ArgumentCaptor<Alerta> salvo = ArgumentCaptor.forClass(Alerta.class);
        verify(alertaRepository).save(salvo.capture());
        assertEquals(NivelAlerta.CRITICO, salvo.getValue().getNivel());
        assertEquals(70.0, salvo.getValue().getValorPrevisto(), 0.0001);
        assertEquals(60.0, salvo.getValue().getLimiteUltrapassado(), 0.0001);
        assertTrue(analise.alertaEmitido().isPresent());
    }

    @Test
    void naoDeveEmitirAlertaComHistoricoInsuficiente() {
        // Arrange: valores altos, mas só 3 leituras (mínimo 5): o sistema se cala
        when(sensor.getCodigo()).thenReturn("PLU-01");
        when(sensor.getTipo()).thenReturn(TipoSensor.PLUVIOMETRO);
        when(medicaoRepository.findByCodigoSensorOrderByDataHoraAscIdAsc("PLU-01"))
                .thenReturn(HistoricoFake.de("PLU-01", TipoSensor.PLUVIOMETRO, 90, 95, 100));

        // Act
        AnalisePrevisao analise = servico.analisar(sensor, TipoEstrategia.MEDIA_MOVEL);

        // Assert
        verify(alertaRepository, never()).save(any());
        assertNull(analise.alerta());
    }

    @Test
    void naoDeveGravarMedicaoQuandoSensorFalha() throws FonteIndisponivelException {
        // Arrange
        when(sensor.lerValor()).thenThrow(new FonteIndisponivelException("sensor PLU-01 não respondeu"));

        // Act + Assert
        assertThrows(FonteIndisponivelException.class, () -> servico.registrarLeitura(sensor));
        verify(medicaoRepository, never()).save(any());
    }

    @Test
    void naoDeveGravarMedicaoComValorInvalido() throws FonteIndisponivelException {
        // Arrange: leitura corrompida (chuva negativa)
        when(sensor.getCodigo()).thenReturn("PLU-01");
        when(sensor.getTipo()).thenReturn(TipoSensor.PLUVIOMETRO);
        when(sensor.lerValor()).thenReturn(-4.0);

        // Act + Assert
        assertThrows(DadosInvalidosException.class, () -> servico.registrarLeitura(sensor));
        verify(medicaoRepository, never()).save(any(Medicao.class));
    }

    @Test
    void deveRecusarAlteracaoDeLimiarComCodigoErrado() throws DadosInvalidosException {
        // Arrange
        DadosTerritorio territorio = new DadosTerritorio("Teste", 1000, 10, "2026");
        Limiar novo = new Limiar(1, 2, 3);
        Limiar original = servico.getConfiguracao().getLimiar(TipoSensor.PLUVIOMETRO);

        // Act + Assert
        assertThrows(DadosInvalidosException.class,
                () -> servico.alterarLimiar(territorio, "0000", TipoSensor.PLUVIOMETRO, novo));
        assertEquals(original, servico.getConfiguracao().getLimiar(TipoSensor.PLUVIOMETRO));
        verify(limiarRepository, never()).save(any());
    }

    @Test
    void naoDeveGravarAlertaRepetidoSemLeiturasNovas() throws DadosInvalidosException {
        // Arrange: última leitura às 04:00 e já existe um CRÍTICO da mesma estratégia às 05:00
        when(sensor.getCodigo()).thenReturn("PLU-01");
        when(sensor.getTipo()).thenReturn(TipoSensor.PLUVIOMETRO);
        when(medicaoRepository.findByCodigoSensorOrderByDataHoraAscIdAsc("PLU-01"))
                .thenReturn(HistoricoFake.de("PLU-01", TipoSensor.PLUVIOMETRO, 60, 65, 70, 75, 80));
        Alerta anterior = new Alerta("PLU-01", TipoSensor.PLUVIOMETRO, NivelAlerta.CRITICO, 70, 60, MEDIA_MOVEL,
                LocalDateTime.of(2026, 11, 19, 5, 0));
        when(alertaRepository.findFirstByCodigoSensorAndEstrategiaOrderByDataHoraDescIdDesc(eq("PLU-01"),
                eq(MEDIA_MOVEL))).thenReturn(Optional.of(anterior));

        // Act
        AnalisePrevisao analise = servico.analisar(sensor, TipoEstrategia.MEDIA_MOVEL);

        // Assert
        verify(alertaRepository, never()).save(any());
        assertTrue(analise.alertaRepetido());
        assertSame(anterior, analise.alerta());
    }

    @Test
    void deveGravarNovoAlertaQuandoChegouLeituraDepoisDoAnterior() throws DadosInvalidosException {
        // Arrange: alerta anterior às 02:00, mas há leituras até 04:00
        when(sensor.getCodigo()).thenReturn("PLU-01");
        when(sensor.getTipo()).thenReturn(TipoSensor.PLUVIOMETRO);
        when(medicaoRepository.findByCodigoSensorOrderByDataHoraAscIdAsc("PLU-01"))
                .thenReturn(HistoricoFake.de("PLU-01", TipoSensor.PLUVIOMETRO, 60, 65, 70, 75, 80));
        Alerta anterior = new Alerta("PLU-01", TipoSensor.PLUVIOMETRO, NivelAlerta.CRITICO, 65, 60, MEDIA_MOVEL,
                LocalDateTime.of(2026, 11, 19, 2, 0));
        when(alertaRepository.findFirstByCodigoSensorAndEstrategiaOrderByDataHoraDescIdDesc(eq("PLU-01"),
                eq(MEDIA_MOVEL))).thenReturn(Optional.of(anterior));
        when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        AnalisePrevisao analise = servico.analisar(sensor, TipoEstrategia.MEDIA_MOVEL);

        // Assert
        verify(alertaRepository).save(any(Alerta.class));
        assertFalse(analise.alertaRepetido());
        assertEquals(70.0, analise.alerta().getValorPrevisto(), 0.0001);
    }

    @Test
    void deveGravarLimiarAlteradoNoBanco() throws DadosInvalidosException {
        // Arrange
        DadosTerritorio territorio = new DadosTerritorio("Teste", 1000, 10, "2026");
        Limiar novo = new Limiar(10, 30, 50);

        // Act
        servico.alterarLimiar(territorio, "2026", TipoSensor.PLUVIOMETRO, novo);

        // Assert
        ArgumentCaptor<LimiarSalvo> salvo = ArgumentCaptor.forClass(LimiarSalvo.class);
        verify(limiarRepository).save(salvo.capture());
        assertEquals(TipoSensor.PLUVIOMETRO, salvo.getValue().getTipoSensor());
        assertEquals(50.0, salvo.getValue().paraLimiar().getCritico(), 0.0001);
        assertSame(novo, servico.getConfiguracao().getLimiar(TipoSensor.PLUVIOMETRO));
    }

    @Test
    void deveAplicarLimiaresGravadosAoIniciar() throws DadosInvalidosException {
        // Arrange: o banco tem um limiar de umidade alterado numa execução anterior
        LimiarSalvo umidade = new LimiarSalvo(TipoSensor.UMIDADE_SOLO, new Limiar(60, 75, 90),
                LocalDateTime.of(2026, 11, 18, 10, 0));
        when(limiarRepository.findAll()).thenReturn(List.of(umidade));

        // Act
        int aplicados = servico.carregarLimiaresSalvos();

        // Assert
        assertEquals(1, aplicados);
        assertEquals(90.0, servico.getConfiguracao().getLimiar(TipoSensor.UMIDADE_SOLO).getCritico(), 0.0001);
        assertEquals(60.0, servico.getConfiguracao().getLimiar(TipoSensor.PLUVIOMETRO).getCritico(), 0.0001,
                "tipos sem limiar gravado continuam no padrão");
    }
}
