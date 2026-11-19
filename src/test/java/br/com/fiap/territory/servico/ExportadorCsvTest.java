package br.com.fiap.territory.servico;

import br.com.fiap.territory.dominio.HistoricoFake;
import br.com.fiap.territory.dominio.TipoSensor;
import br.com.fiap.territory.excecao.DadosInvalidosException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExportadorCsvTest {

    @TempDir
    Path pastaTemporaria;

    private ExportadorCsv exportador;

    @BeforeEach
    void prepararExportador() {
        Clock relogioFixo = Clock.fixed(Instant.parse("2026-11-19T12:00:00Z"), ZoneId.of("UTC"));
        exportador = new ExportadorCsv(pastaTemporaria.resolve("exportacoes"), relogioFixo);
    }

    @Test
    void deveGravarCabecalhoEUmaLinhaPorMedicao() throws DadosInvalidosException, IOException {
        // Arrange
        var historico = HistoricoFake.de("PLU-01", TipoSensor.PLUVIOMETRO, 12.5, 40);

        // Act
        Path arquivo = exportador.exportarHistorico("PLU-01", historico);

        // Assert
        List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        assertEquals("PLU-01_20261119-120000.csv", arquivo.getFileName().toString());
        assertEquals(List.of(
                ExportadorCsv.CABECALHO,
                "19/11/2026 00:00:00;PLU-01;PLUVIOMETRO;12,50;mm/h",
                "19/11/2026 01:00:00;PLU-01;PLUVIOMETRO;40,00;mm/h"), linhas);
    }

    @Test
    void deveRecusarExportarHistoricoVazio() {
        // Act + Assert
        assertThrows(DadosInvalidosException.class, () -> exportador.exportarHistorico("PLU-01", List.of()));
        assertFalse(Files.exists(pastaTemporaria.resolve("exportacoes")), "nenhum arquivo deve ser criado");
    }
}
