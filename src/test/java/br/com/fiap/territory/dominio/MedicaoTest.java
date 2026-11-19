package br.com.fiap.territory.dominio;

import br.com.fiap.territory.excecao.DadosInvalidosException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedicaoTest {

    @Test
    void deveCriarMedicaoQuandoValorEstaDentroDaFaixa() throws DadosInvalidosException {
        // Arrange
        LocalDateTime agora = LocalDateTime.of(2026, 11, 19, 10, 0);

        // Act
        Medicao medicao = new Medicao("PLU-01", TipoSensor.PLUVIOMETRO, 32.5, agora);

        // Assert
        assertEquals("PLU-01", medicao.getCodigoSensor());
        assertEquals(32.5, medicao.getValor(), 0.0001);
        assertEquals(TipoSensor.PLUVIOMETRO, medicao.getTipoSensor());
    }

    @Test
    void deveLancarDadosInvalidosQuandoChuvaForNegativa() {
        // Arrange
        LocalDateTime agora = LocalDateTime.now();

        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> new Medicao("PLU-01", TipoSensor.PLUVIOMETRO, -3.0, agora));

        // Assert
        assertTrue(erro.getMessage().contains("pluviômetro"), "a mensagem deve dizer qual medição falhou");
    }

    @Test
    void deveLancarDadosInvalidosQuandoUmidadePassarDe100() {
        // Arrange
        LocalDateTime agora = LocalDateTime.now();

        // Act + Assert
        assertThrows(DadosInvalidosException.class,
                () -> new Medicao("UMI-01", TipoSensor.UMIDADE_SOLO, 120.0, agora));
    }
}
