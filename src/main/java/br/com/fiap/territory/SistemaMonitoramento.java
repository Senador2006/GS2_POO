package br.com.fiap.territory;

import br.com.fiap.territory.dominio.Alerta;
import br.com.fiap.territory.dominio.DadosTerritorio;
import br.com.fiap.territory.dominio.Limiar;
import br.com.fiap.territory.dominio.Medicao;
import br.com.fiap.territory.dominio.PontoMonitoramento;
import br.com.fiap.territory.dominio.ResultadoPrevisao;
import br.com.fiap.territory.dominio.Sensor;
import br.com.fiap.territory.dominio.TipoSensor;
import br.com.fiap.territory.excecao.DadosInvalidosException;
import br.com.fiap.territory.excecao.FonteIndisponivelException;
import br.com.fiap.territory.servico.AnalisePrevisao;
import br.com.fiap.territory.servico.CatalogoTerritorio;
import br.com.fiap.territory.servico.ExportadorCsv;
import br.com.fiap.territory.servico.RelatorioBuilder;
import br.com.fiap.territory.servico.ServicoMonitoramento;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.dao.DataAccessException;

import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/**
 * Ponto de entrada + menu de console.
 * O menu só conversa com o usuário: lê opções, chama o serviço e mostra o resultado.
 * Todo erro que sobe do serviço é capturado AQUI com uma mensagem clara (nenhum catch vazio).
 */
@SpringBootApplication
public class SistemaMonitoramento implements CommandLineRunner {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final ServicoMonitoramento servico;
    private final CatalogoTerritorio catalogo;
    private final ExportadorCsv exportadorCsv;
    private final Scanner entrada = new Scanner(System.in);

    public SistemaMonitoramento(ServicoMonitoramento servico, CatalogoTerritorio catalogo,
                                ExportadorCsv exportadorCsv) {
        this.servico = servico;
        this.catalogo = catalogo;
        this.exportadorCsv = exportadorCsv;
    }

    public static void main(String[] args) {
        SpringApplication.run(SistemaMonitoramento.class, args);
    }

    @Override
    public void run(String... args) {
        System.out.println("=== Territory Predictive Insights - chuva e risco de alagamento ===");
        System.out.println("Território: " + catalogo.getDadosTerritorio());
        carregarLimiaresSalvos();
        int opcao;
        do {
            exibirMenu();
            opcao = lerInteiro("Escolha uma opção: ");
            try {
                switch (opcao) {
                    case 1 -> verificarSensores();
                    case 2 -> rodarPrevisoes();
                    case 3 -> verHistorico();
                    case 4 -> gerenciarDadosTerritorio();
                    case 5 -> statusCompleto();
                    case 0 -> System.out.println("Encerrando. Os dados ficam salvos em ./data para a próxima execução.");
                    default -> System.out.println("Opção inválida: digite um número de 0 a 5.");
                }
            } catch (DataAccessException e) {
                System.out.println("Erro ao acessar o banco de dados: " + e.getMostSpecificCause().getMessage());
            }
        } while (opcao != 0);
    }

    private void carregarLimiaresSalvos() {
        try {
            int personalizados = servico.carregarLimiaresSalvos();
            if (personalizados > 0) {
                System.out.println(personalizados + " limiar(es) alterado(s) em execuções anteriores carregado(s) do banco.");
            }
        } catch (DadosInvalidosException e) {
            System.out.println("Limiares gravados no banco ignorados (" + e.getMessage() + "). Usando os valores padrão.");
        }
    }

    private void exibirMenu() {
        System.out.println();
        System.out.println("1 - Verificar sensores (ler e gravar medições)");
        System.out.println("2 - Rodar previsões e alertas");
        System.out.println("3 - Ver histórico (medições por sensor / alertas emitidos / exportar CSV)");
        System.out.println("4 - Gerenciar dados do território");
        System.out.println("5 - Status completo");
        System.out.println("0 - Sair");
    }

    // ------------------------------------------------------------------ 1
    private void verificarSensores() {
        int gravadas = 0;
        for (PontoMonitoramento ponto : catalogo.getPontos()) {
            System.out.println("\n" + ponto.getId() + " - " + ponto.getNome());
            for (Sensor sensor : ponto.getSensores()) {
                try {
                    Medicao medicao = servico.registrarLeitura(sensor);
                    System.out.println("  OK    " + medicao);
                    gravadas++;
                } catch (FonteIndisponivelException e) {
                    System.out.println("  FALHA Fonte indisponível: " + e.getMessage());
                } catch (DadosInvalidosException e) {
                    System.out.println("  DESCARTADA Dados inválidos: " + e.getMessage() + " (não gravada)");
                }
            }
            System.out.println("  Status do ponto: " + ponto.verificarStatus().getDescricao());
        }
        System.out.println("\n" + gravadas + " medição(ões) gravada(s) no banco.");
    }

    // ------------------------------------------------------------------ 2
    private void rodarPrevisoes() {
        int novos = 0;
        int repetidos = 0;
        for (Sensor sensor : catalogo.getTodosSensores()) {
            System.out.println("\nSensor " + sensor.getCodigo() + " (" + sensor.getTipo().getDescricao() + ")");
            for (AnalisePrevisao analise : servico.analisarComTodasEstrategias(sensor)) {
                ResultadoPrevisao r = analise.resultado();
                System.out.println("  - " + r.estrategia() + ": " + r.significado());
                if (analise.alertaEmitido().isEmpty()) {
                    continue;
                }
                if (analise.alertaRepetido()) {
                    System.out.println("    >> (mantido: já emitido em " + analise.alerta().getDataHora().format(FORMATO_DATA)
                            + ", sem leituras novas desde então) " + analise.alerta().explicar());
                    repetidos++;
                } else {
                    System.out.println("    >> " + analise.alerta().explicar());
                    novos++;
                }
            }
        }
        System.out.println("\n" + novos + " alerta(s) novo(s) emitido(s) e gravado(s).");
        if (repetidos > 0) {
            System.out.println(repetidos + " alerta(s) mantido(s) sem gravar de novo. Use a opção 1 para obter leituras novas.");
        }
    }

    // ------------------------------------------------------------------ 3
    private void verHistorico() {
        System.out.println("1 - Medições de um sensor");
        System.out.println("2 - Alertas já emitidos");
        System.out.println("3 - Exportar medições de um sensor em CSV");
        int opcao = lerInteiro("Opção: ");
        switch (opcao) {
            case 1 -> listarMedicoesDeSensor();
            case 2 -> listarAlertas();
            case 3 -> exportarMedicoesEmCsv();
            default -> System.out.println("Opção inválida.");
        }
    }

    private Sensor escolherSensor() throws FonteIndisponivelException {
        System.out.print("Sensores: ");
        catalogo.getTodosSensores().forEach(s -> System.out.print(s.getCodigo() + "  "));
        System.out.println();
        return catalogo.buscarSensor(lerTexto("Código do sensor: "));
    }

    private void listarMedicoesDeSensor() {
        try {
            Sensor sensor = escolherSensor();
            List<Medicao> historico = servico.buscarHistoricoPorSensor(sensor.getCodigo());
            if (historico.isEmpty()) {
                System.out.println("Nenhuma medição gravada para " + sensor.getCodigo() + ". Use a opção 1 antes.");
                return;
            }
            historico.forEach(m -> System.out.println("  " + m));
            System.out.println(historico.size() + " medição(ões).");
        } catch (FonteIndisponivelException e) {
            System.out.println("Não foi possível consultar: " + e.getMessage());
        }
    }

    private void exportarMedicoesEmCsv() {
        try {
            Sensor sensor = escolherSensor();
            List<Medicao> historico = servico.buscarHistoricoPorSensor(sensor.getCodigo());
            Path arquivo = exportadorCsv.exportarHistorico(sensor.getCodigo(), historico);
            System.out.println(historico.size() + " medição(ões) exportada(s) para " + arquivo.toAbsolutePath());
        } catch (FonteIndisponivelException e) {
            System.out.println("Não foi possível consultar: " + e.getMessage());
        } catch (DadosInvalidosException e) {
            System.out.println("Nada exportado: " + e.getMessage() + ". Use a opção 1 antes.");
        } catch (IOException e) {
            System.out.println("Não foi possível gravar o arquivo CSV: " + e.getMessage());
        }
    }

    private void listarAlertas() {
        List<Alerta> alertas = servico.listarAlertas();
        if (alertas.isEmpty()) {
            System.out.println("Nenhum alerta emitido até agora.");
            return;
        }
        alertas.forEach(a -> System.out.println("  " + a));
    }

    // ------------------------------------------------------------------ 4
    private void gerenciarDadosTerritorio() {
        DadosTerritorio dados = catalogo.getDadosTerritorio();
        System.out.println("Dados atuais: " + dados);
        System.out.println("1 - Alterar população estimada");
        System.out.println("2 - Alterar domicílios em área de risco");
        System.out.println("3 - Alterar limiar de um tipo de sensor (exige código de acesso)");
        System.out.println("4 - Restaurar todos os limiares padrão (exige código de acesso)");
        int opcao = lerInteiro("Opção: ");
        try {
            switch (opcao) {
                case 1 -> dados.setPopulacaoEstimada(lerInteiro("Nova população estimada: "));
                case 2 -> dados.setDomiciliosEmAreaDeRisco(lerInteiro("Novos domicílios em área de risco: "));
                case 3 -> alterarLimiar(dados);
                case 4 -> servico.restaurarLimiaresPadrao(dados, lerTexto("Código de acesso: "));
                default -> {
                    System.out.println("Opção inválida.");
                    return;
                }
            }
            System.out.println("Dados atualizados com sucesso.");
        } catch (DadosInvalidosException e) {
            System.out.println("Alteração recusada: " + e.getMessage());
        }
    }

    private void alterarLimiar(DadosTerritorio dados) throws DadosInvalidosException {
        TipoSensor[] tipos = TipoSensor.values();
        for (int i = 0; i < tipos.length; i++) {
            System.out.printf("%d - %s (%s) | atual: %s%n", i + 1, tipos[i].getDescricao(), tipos[i].getUnidade(),
                    servico.getConfiguracao().getLimiar(tipos[i]));
        }
        int indice = lerInteiro("Tipo de sensor: ") - 1;
        if (indice < 0 || indice >= tipos.length) {
            throw new DadosInvalidosException("tipo de sensor inexistente");
        }
        String codigo = lerTexto("Código de acesso: ");
        dados.validarCodigoAcesso(codigo); // recusa logo, antes de pedir os valores
        Limiar novo = new Limiar(lerDecimal("Limite de ATENÇÃO: "), lerDecimal("Limite de ALERTA: "),
                lerDecimal("Limite CRÍTICO: "));
        servico.alterarLimiar(dados, codigo, tipos[indice], novo);
    }

    // ------------------------------------------------------------------ 5
    private void statusCompleto() {
        catalogo.getPontos().forEach(PontoMonitoramento::verificarStatus);
        String relatorio = new RelatorioBuilder("STATUS COMPLETO")
                .comTerritorio(catalogo.getDadosTerritorio())
                .comPontos(catalogo.getPontos())
                .comLimiares(servico.getConfiguracao().getLimiares())
                .comTotaisDoBanco(servico.contarMedicoes(), servico.contarAlertas())
                .comUltimosAlertas(servico.listarAlertas(), 3)
                .construir();
        System.out.println("\n" + relatorio);
    }

    // ------------------------------------------------------------------ leitura do teclado
    private int lerInteiro(String rotulo) {
        while (true) {
            String texto = lerTexto(rotulo);
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                System.out.println("\"" + texto + "\" não é um número inteiro. Tente de novo.");
            }
        }
    }

    private double lerDecimal(String rotulo) {
        while (true) {
            String texto = lerTexto(rotulo).replace(',', '.');
            try {
                return Double.parseDouble(texto);
            } catch (NumberFormatException e) {
                System.out.println("\"" + texto + "\" não é um número. Use, por exemplo, 12.5");
            }
        }
    }

    private String lerTexto(String rotulo) {
        System.out.print(rotulo);
        if (!entrada.hasNextLine()) {
            // Entrada fechada (Ctrl+Z / fim do arquivo): encerra o programa em vez de entrar em laço infinito.
            System.out.println("\nEntrada encerrada. Saindo.");
            System.exit(0);
        }
        return entrada.nextLine().trim();
    }
}
