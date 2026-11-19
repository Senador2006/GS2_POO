package br.com.fiap.territory.servico;

import br.com.fiap.territory.dominio.DadosTerritorio;
import br.com.fiap.territory.dominio.PontoMonitoramento;
import br.com.fiap.territory.dominio.Sensor;
import br.com.fiap.territory.dominio.SensorNivelCorrego;
import br.com.fiap.territory.dominio.SensorPluviometrico;
import br.com.fiap.territory.dominio.SensorUmidadeSolo;
import br.com.fiap.territory.excecao.DadosInvalidosException;
import br.com.fiap.territory.excecao.FonteIndisponivelException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Monta o território de exemplo: dados gerais + pontos vulneráveis com seus sensores.
 * Os sensores ficam em memória (são "equipamentos"); o que vai para o banco são as
 * MEDIÇÕES e os ALERTAS que eles geram.
 *
 * Nomes fictícios, sem endereços nem dados pessoais.
 */
@Component
public class CatalogoTerritorio {

    private final DadosTerritorio dadosTerritorio;
    private final List<PontoMonitoramento> pontos = new ArrayList<>();

    public CatalogoTerritorio(@Value("${territorio.codigo-acesso}") String codigoAcesso) {
        try {
            this.dadosTerritorio = new DadosTerritorio("Bacia do Córrego Esperança", 18500, 420, codigoAcesso);

            PontoMonitoramento corrego = new PontoMonitoramento("PT-01", "Margem do córrego - trecho baixo", "Zona Leste");
            corrego.adicionarSensor(new SensorPluviometrico("PLU-01"));
            corrego.adicionarSensor(new SensorNivelCorrego("NIV-01"));

            PontoMonitoramento encosta = new PontoMonitoramento("PT-02", "Encosta da vila alta", "Zona Norte");
            encosta.adicionarSensor(new SensorPluviometrico("PLU-02"));
            encosta.adicionarSensor(new SensorUmidadeSolo("UMI-01"));

            PontoMonitoramento passagem = new PontoMonitoramento("PT-03", "Passagem subterrânea central", "Centro");
            passagem.adicionarSensor(new SensorPluviometrico("PLU-03"));
            passagem.adicionarSensor(new SensorNivelCorrego("NIV-02"));

            pontos.add(corrego);
            pontos.add(encosta);
            pontos.add(passagem);
        } catch (DadosInvalidosException e) {
            throw new IllegalStateException("território de exemplo mal configurado: " + e.getMessage(), e);
        }
    }

    public DadosTerritorio getDadosTerritorio() {
        return dadosTerritorio;
    }

    public List<PontoMonitoramento> getPontos() {
        return Collections.unmodifiableList(pontos);
    }

    public List<Sensor> getTodosSensores() {
        return pontos.stream().flatMap(p -> p.getSensores().stream()).toList();
    }

    public Sensor buscarSensor(String codigo) throws FonteIndisponivelException {
        return getTodosSensores().stream()
                .filter(s -> s.getCodigo().equalsIgnoreCase(codigo == null ? "" : codigo.trim()))
                .findFirst()
                .orElseThrow(() -> new FonteIndisponivelException("não existe sensor com o código \"" + codigo + "\""));
    }
}
