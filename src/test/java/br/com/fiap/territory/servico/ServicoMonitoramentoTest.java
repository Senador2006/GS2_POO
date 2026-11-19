package br.com.fiap.territory.servico;

import br.com.fiap.territory.dominio.Alerta;
import br.com.fiap.territory.dominio.DadosTerritorio;
import br.com.fiap.territory.dominio.HistoricoFake;
import br.com.fiap.territory.dominio.Limiar;
import br.com.fiap.territory.dominio.Medicao;
import br.com.fiap.territory.dominio.NivelAlerta;
import br.com.fiap.territory.dominio.Sensor;
import br.com.fiap.territory.dominio.TipoEstrategia;
import br.com.fiap.territory.dominio.TipoSensor;
import br.com.fiap.territory.excecao.DadosInvalidosException;
import br.com.fiap.territory.excecao.FonteIndisponivelException;
import br.com.fiap.territory.persistencia.AlertaRepository;
import br.com.fiap.territory.persistencia.MedicaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Testa a lógica de alerta SEM banco: os repositories são mocks do Mockito. */
@ExtendWith(MockitoExtension.class)
class ServicoMonitoramentoTest {

    @Mock
    private MedicaoRepository medicaoRepository;

    @Mock
    private AlertaRepository alertaRepository;

    @Mock
    private Sensor sensor;

    private ServicoMonitoramento servico;

    @BeforeEach
    void prepararServico() {
        ConfiguracaoSistema configuracao = ConfiguracaoSistema.getInstancia();
        configuracao.restaurarPadroes(); // pluviômetro: 20 / 40 / 60 mm/h
        Clock relogioFixo = Clock.fixed(Instant.parse("2026-11-19T12:00:00Z"), ZoneId.of("UTC"));
        servico = new ServicoMonitoramento(medicaoRepository, alertaRepository,
                new FabricaPrevisao(configuracao), configuracao, relogioFixo);
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
    }
}
