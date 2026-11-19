package br.com.fiap.territory.dominio;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrevisaoComparacaoTest {

    @Test
    void deveProjetarATendenciaDeAlta() {
        // Arrange: anterior = média(1,1,1) = 1 ; recente = média(2,2,2) = 2
        PrevisaoComparacao estrategia = new PrevisaoComparacao(3);
        List<Medicao> historico = HistoricoFake.de("NIV-01", TipoSensor.NIVEL_CORREGO,
                1, 1, 1, 2, 2, 2);

        // Act
        ResultadoPrevisao resultado = estrategia.prever(historico);

        // Assert: previsto = 2 + (2 - 1) = 3
        assertTrue(resultado.confiavel());
        assertEquals(3.0, resultado.valorPrevisto(), 0.0001);
        assertTrue(resultado.significado().contains("alta"));
    }

    @Test
    void naoDevePreverValorNegativoEmTendenciaDeQueda() {
        // Arrange: anterior = 9, recente = 1 -> 1 + (1 - 9) = -7, mas chuva não fica negativa
        PrevisaoComparacao estrategia = new PrevisaoComparacao(3);
        List<Medicao> historico = HistoricoFake.de("PLU-01", TipoSensor.PLUVIOMETRO,
                9, 9, 9, 1, 1, 1);

        // Act
        ResultadoPrevisao resultado = estrategia.prever(historico);

        // Assert
        assertEquals(0.0, resultado.valorPrevisto(), 0.0001);
    }
}
