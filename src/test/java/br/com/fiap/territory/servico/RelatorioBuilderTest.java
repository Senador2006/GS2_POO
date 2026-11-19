package br.com.fiap.territory.servico;

import br.com.fiap.territory.dominio.DadosTerritorio;
import br.com.fiap.territory.excecao.DadosInvalidosException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelatorioBuilderTest {

    @Test
    void deveMontarSoAsSecoesPedidasNaOrdemPedida() throws DadosInvalidosException {
        // Arrange
        DadosTerritorio territorio = new DadosTerritorio("Bacia Teste", 1000, 10, "1234");

        // Act
        String relatorio = new RelatorioBuilder("STATUS")
                .comTotaisDoBanco(7, 2)
                .comTerritorio(territorio)
                .comUltimosAlertas(List.of(), 3)
                .construir();

        // Assert
        assertTrue(relatorio.startsWith("--- STATUS ---"));
        assertTrue(relatorio.indexOf("Banco: 7 medição(ões) e 2 alerta(s)") < relatorio.indexOf("Território: Bacia Teste"));
        assertTrue(relatorio.contains("nenhum alerta emitido"));
        assertFalse(relatorio.contains("Limiares em uso"), "seção não pedida não aparece");
    }

    @Test
    void deveRecusarRelatorioSemNenhumaSecao() {
        // Arrange
        RelatorioBuilder vazio = new RelatorioBuilder("STATUS");

        // Act + Assert
        IllegalStateException erro = assertThrows(IllegalStateException.class, vazio::construir);
        assertEquals("relatório \"STATUS\" sem nenhuma seção", erro.getMessage());
    }
}
