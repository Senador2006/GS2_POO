package br.com.fiap.territory.servico;

import br.com.fiap.territory.dominio.Medicao;
import br.com.fiap.territory.excecao.DadosInvalidosException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Exporta o histórico de um sensor para CSV (bônus do enunciado).
 * Separador ";" e vírgula decimal: o arquivo abre direto no Excel em português.
 * O arquivo é fechado pelo try-with-resources mesmo se der erro no meio da escrita.
 */
@Component
public class ExportadorCsv {

    private static final DateTimeFormatter FORMATO_NOME_ARQUIVO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    static final String CABECALHO = "data_hora;codigo_sensor;tipo_sensor;valor;unidade";

    private final Path pasta;
    private final Clock relogio;

    @Autowired
    public ExportadorCsv(@Value("${territorio.pasta-exportacao}") String pasta) {
        this(Path.of(pasta), Clock.systemDefaultZone());
    }

    /** Construtor com pasta e relógio controláveis (usado nos testes). */
    ExportadorCsv(Path pasta, Clock relogio) {
        this.pasta = Objects.requireNonNull(pasta, "pasta");
        this.relogio = Objects.requireNonNull(relogio, "relogio");
    }

    /**
     * Grava as medições em {@code <pasta>/<codigo>_<data-hora>.csv}.
     *
     * @return caminho do arquivo criado
     * @throws DadosInvalidosException se não houver medições para exportar
     * @throws IOException             se não for possível criar a pasta ou escrever o arquivo
     */
    public Path exportarHistorico(String codigoSensor, List<Medicao> historico)
            throws DadosInvalidosException, IOException {
        if (historico == null || historico.isEmpty()) {
            throw new DadosInvalidosException("não há medições do sensor " + codigoSensor + " para exportar");
        }
        Files.createDirectories(pasta);
        Path arquivo = pasta.resolve(codigoSensor + "_" + LocalDateTime.now(relogio).format(FORMATO_NOME_ARQUIVO)
                + ".csv");
        try (BufferedWriter escritor = Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8)) {
            escritor.write(CABECALHO);
            escritor.newLine();
            for (Medicao medicao : historico) {
                escritor.write(linhaDe(medicao));
                escritor.newLine();
            }
        }
        return arquivo;
    }

    private static String linhaDe(Medicao medicao) {
        return String.join(";",
                medicao.getDataHora().format(FORMATO_DATA),
                medicao.getCodigoSensor(),
                medicao.getTipoSensor().name(),
                String.format(PT_BR, "%.2f", medicao.getValor()),
                medicao.getTipoSensor().getUnidade());
    }
}
