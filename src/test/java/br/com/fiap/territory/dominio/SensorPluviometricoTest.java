package br.com.fiap.territory.dominio;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SensorPluviometricoTest {

    @Test
    void deveGerarChuvaForteEmEpisodiosDentroDaFaixa() {
        // Arrange: semente fixa para o teste ser repetível
        Random random = new Random(42);
        SensorPluviometrico sensor = new SensorPluviometrico("PLU-T", random, 0);

        // Act
        int maiorSequenciaForte = 0;
        int sequenciaAtual = 0;
        double menor = Double.MAX_VALUE;
        double maior = -Double.MAX_VALUE;
        for (int i = 0; i < 500; i++) {
            double valor = sensor.gerarValor(random);
            menor = Math.min(menor, valor);
            maior = Math.max(maior, valor);
            sequenciaAtual = valor >= SensorPluviometrico.LIMITE_CHUVA_FRACA ? sequenciaAtual + 1 : 0;
            maiorSequenciaForte = Math.max(maiorSequenciaForte, sequenciaAtual);
        }

        // Assert
        assertTrue(menor >= 0, "chuva nunca negativa na simulação normal");
        assertTrue(maior <= SensorPluviometrico.LIMITE_CHUVA_FORTE, "chuva acima de 85 mm/h não é simulada");
        assertTrue(maiorSequenciaForte >= 3, "deve haver pancadas fortes que duram várias leituras seguidas");
    }
}
