package br.com.fiap.territory.servico;

import br.com.fiap.territory.dominio.EstrategiaPrevisao;
import br.com.fiap.territory.dominio.PrevisaoComparacao;
import br.com.fiap.territory.dominio.PrevisaoMediaMovel;
import br.com.fiap.territory.dominio.TipoEstrategia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class FabricaPrevisaoTest {

    private FabricaPrevisao fabrica;

    @BeforeEach
    void prepararConfiguracao() {
        ConfiguracaoSistema.getInstancia().restaurarPadroes();
        fabrica = new FabricaPrevisao(ConfiguracaoSistema.getInstancia());
    }

    @Test
    void deveCriarAEstrategiaPedidaComAJanelaDaConfiguracao() {
        // Act
        EstrategiaPrevisao media = fabrica.criar(TipoEstrategia.MEDIA_MOVEL);
        EstrategiaPrevisao comparacao = fabrica.criar(TipoEstrategia.COMPARACAO);

        // Assert
        assertInstanceOf(PrevisaoMediaMovel.class, media);
        assertEquals(5, ((PrevisaoMediaMovel) media).getJanela());
        assertInstanceOf(PrevisaoComparacao.class, comparacao);
    }

    @Test
    void deveCriarUmaEstrategiaDeCadaTipo() {
        // Act
        List<EstrategiaPrevisao> todas = fabrica.criarTodas();

        // Assert
        assertEquals(TipoEstrategia.values().length, todas.size());
    }

    @Test
    void singletonDeveDevolverSempreAMesmaInstancia() {
        // Act + Assert
        assertSame(ConfiguracaoSistema.getInstancia(), ConfiguracaoSistema.getInstancia());
    }
}
