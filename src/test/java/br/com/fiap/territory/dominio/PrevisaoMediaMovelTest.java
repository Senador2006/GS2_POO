package br.com.fiap.territory.dominio;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrevisaoMediaMovelTest {

    @Test
    void devePreverMediaDasUltimasCincoLeituras() {
        // Arrange: a primeira leitura (100) fica FORA da janela de 5
        PrevisaoMediaMovel estrategia = new PrevisaoMediaMovel(5);
        List<Medicao> historico = HistoricoFake.de("PLU-01", TipoSensor.PLUVIOMETRO,
                100, 10, 20, 30, 40, 50);

        // Act
        ResultadoPrevisao resultado = estrategia.prever(historico);

        // Assert: (10+20+30+40+50)/5 = 30
        assertTrue(resultado.confiavel());
        assertEquals(30.0, resultado.valorPrevisto(), 0.0001);
    }

    @Test
    void deveSeCalarQuandoHaMenosLeiturasQueAJanela() {
        // Arrange
        PrevisaoMediaMovel estrategia = new PrevisaoMediaMovel(5);
        List<Medicao> historico = HistoricoFake.de("PLU-01", TipoSensor.PLUVIOMETRO, 10, 20, 30);

        // Act
        ResultadoPrevisao resultado = estrategia.prever(historico);

        // Assert
        assertFalse(resultado.confiavel());
        assertTrue(Double.isNaN(resultado.valorPrevisto()));
        assertTrue(resultado.significado().contains("3 de 5"));
    }
}
