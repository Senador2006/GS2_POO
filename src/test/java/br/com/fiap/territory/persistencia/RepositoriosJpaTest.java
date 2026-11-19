package br.com.fiap.territory.persistencia;

import br.com.fiap.territory.SistemaMonitoramento;
import br.com.fiap.territory.dominio.Alerta;
import br.com.fiap.territory.dominio.Limiar;
import br.com.fiap.territory.dominio.LimiarSalvo;
import br.com.fiap.territory.dominio.Medicao;
import br.com.fiap.territory.dominio.NivelAlerta;
import br.com.fiap.territory.dominio.TipoSensor;
import br.com.fiap.territory.excecao.DadosInvalidosException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de integração: grava e lê de verdade num H2 em memória (o @DataJpaTest troca o banco
 * em arquivo por um temporário, então ./data não é tocado).
 */
@DataJpaTest
class RepositoriosJpaTest {

    /**
     * Configuração mínima só com JPA. Sem ela o teste carregaria o SistemaMonitoramento,
     * que precisa do serviço e abriria o menu de console.
     */
    @SpringBootConfiguration
    @AutoConfigurationPackage(basePackageClasses = SistemaMonitoramento.class)
    static class ConfiguracaoSoJpa {
    }

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 19, 8, 0);

    @Autowired
    private MedicaoRepository medicaoRepository;

    @Autowired
    private AlertaRepository alertaRepository;

    @Autowired
    private LimiarRepository limiarRepository;

    @Test
    void deveDevolverHistoricoDoSensorEmOrdemCronologica() throws DadosInvalidosException {
        // Arrange: gravadas fora de ordem e misturadas com outro sensor
        medicaoRepository.save(new Medicao("PLU-01", TipoSensor.PLUVIOMETRO, 30, INICIO.plusHours(2)));
        medicaoRepository.save(new Medicao("NIV-01", TipoSensor.NIVEL_CORREGO, 1.2, INICIO.plusHours(1)));
        medicaoRepository.save(new Medicao("PLU-01", TipoSensor.PLUVIOMETRO, 10, INICIO));

        // Act
        List<Medicao> historico = medicaoRepository.findByCodigoSensorOrderByDataHoraAscIdAsc("PLU-01");

        // Assert
        assertEquals(List.of(10.0, 30.0), historico.stream().map(Medicao::getValor).toList());
        assertEquals(2, medicaoRepository.countByCodigoSensor("PLU-01"));
    }

    @Test
    void deveEncontrarOUltimoAlertaDoSensorPorEstrategia() {
        // Arrange
        alertaRepository.save(new Alerta("PLU-01", TipoSensor.PLUVIOMETRO, NivelAlerta.ATENCAO, 25, 20,
                "média móvel de 5 leituras", INICIO));
        alertaRepository.save(new Alerta("PLU-01", TipoSensor.PLUVIOMETRO, NivelAlerta.CRITICO, 65, 60,
                "média móvel de 5 leituras", INICIO.plusHours(1)));
        alertaRepository.save(new Alerta("PLU-01", TipoSensor.PLUVIOMETRO, NivelAlerta.ALERTA, 45, 40,
                "outra estratégia", INICIO.plusHours(2)));

        // Act
        var ultimo = alertaRepository.findFirstByCodigoSensorAndEstrategiaOrderByDataHoraDescIdDesc(
                "PLU-01", "média móvel de 5 leituras");

        // Assert
        assertTrue(ultimo.isPresent());
        assertEquals(NivelAlerta.CRITICO, ultimo.get().getNivel());
    }

    @Test
    void deveManterUmLimiarPorTipoDeSensor() throws DadosInvalidosException {
        // Arrange
        limiarRepository.save(new LimiarSalvo(TipoSensor.PLUVIOMETRO, new Limiar(10, 20, 30), INICIO));

        // Act: gravar de novo o mesmo tipo substitui a linha anterior
        limiarRepository.save(new LimiarSalvo(TipoSensor.PLUVIOMETRO, new Limiar(15, 25, 35), INICIO.plusHours(1)));

        // Assert
        assertEquals(1, limiarRepository.count());
        assertEquals(35.0, limiarRepository.findById(TipoSensor.PLUVIOMETRO).orElseThrow().paraLimiar().getCritico(),
                0.0001);
    }
}
