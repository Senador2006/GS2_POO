package br.com.fiap.territory.dominio;

import br.com.fiap.territory.excecao.DadosInvalidosException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimiarTest {

    @Test
    void deveClassificarCadaFaixaNoNivelCorreto() throws DadosInvalidosException {
        // Arrange
        Limiar limiar = new Limiar(20, 40, 60);

        // Act
        Optional<NivelAlerta> abaixo = limiar.classificar(19.9);
        Optional<NivelAlerta> atencao = limiar.classificar(20);
        Optional<NivelAlerta> alerta = limiar.classificar(45);
        Optional<NivelAlerta> critico = limiar.classificar(60);

        // Assert
        assertTrue(abaixo.isEmpty());
        assertEquals(NivelAlerta.ATENCAO, atencao.orElseThrow());
        assertEquals(NivelAlerta.ALERTA, alerta.orElseThrow());
        assertEquals(NivelAlerta.CRITICO, critico.orElseThrow());
    }

    @Test
    void deveRecusarLimiarForaDeOrdem() {
        // Arrange + Act + Assert: alerta (10) menor que atenção (20) não faz sentido
        assertThrows(DadosInvalidosException.class, () -> new Limiar(20, 10, 60));
    }

    @Test
    void deveRecusarPopulacaoNegativaNoTerritorio() throws DadosInvalidosException {
        // Arrange
        DadosTerritorio territorio = new DadosTerritorio("Teste", 1000, 10, "1234");

        // Act + Assert
        assertThrows(DadosInvalidosException.class, () -> territorio.setPopulacaoEstimada(-5));
        assertEquals(1000, territorio.getPopulacaoEstimada(), "o valor antigo deve ser mantido");
    }
}
