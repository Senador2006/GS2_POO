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
import br.com.fiap.territory.servico.ServicoMonitoramento;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Ponto de entrada + menu de console.
 * O menu só conversa com o usuário: lê opções, chama o serviço e mostra o resultado.
 * Todo erro que sobe do serviço é capturado AQUI com uma mensagem clara (nenhum catch vazio).
 */
@SpringBootApplication
public class SistemaMonitoramento implements CommandLineRunner {

    private final ServicoMonitoramento servico;
    private final CatalogoTerritorio catalogo;
    private final Scanner entrada = new Scanner(System.in);

    public SistemaMonitoramento(ServicoMonitoramento servico, CatalogoTerritorio catalogo) {
        this.servico = servico;
        this.catalogo = catalogo;
    }

    public static void main(String[] args) {
        SpringApplication.run(SistemaMonitoramento.class, args);
    }

    @Override
    public void run(String... args) {
        System.out.println("=== Territory Predictive Insights - chuva e risco de alagamento ===");
        System.out.println("Território: " + catalogo.getDadosTerritorio());
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

    private void exibirMenu() {
        System.out.println();
        System.out.println("1 - Verificar sensores (ler e gravar medições)");
        System.out.println("2 - Rodar previsões e alertas");
        System.out.println("3 - Ver histórico (medições por sensor / alertas emitidos)");
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
        int alertas = 0;
        for (Sensor sensor : catalogo.getTodosSensores()) {
            System.out.println("\nSensor " + sensor.getCodigo() + " (" + sensor.getTipo().getDescricao() + ")");
            for (AnalisePrevisao analise : servico.analisarComTodasEstrategias(sensor)) {
                ResultadoPrevisao r = analise.resultado();
                System.out.println("  - " + r.estrategia() + ": " + r.significado());
                if (analise.alertaEmitido().isPresent()) {
                    System.out.println("    >> " + analise.alerta().explicar());
                    alertas++;
                }
            }
        }
        System.out.println("\n" + alertas + " alerta(s) emitido(s) e gravado(s).");
    }

    // ------------------------------------------------------------------ 3
    private void verHistorico() {
        System.out.println("1 - Medições de um sensor");
        System.out.println("2 - Alertas já emitidos");
        int opcao = lerInteiro("Opção: ");
        switch (opcao) {
            case 1 -> listarMedicoesDeSensor();
            case 2 -> listarAlertas();
            default -> System.out.println("Opção inválida.");
        }
    }

    private void listarMedicoesDeSensor() {
        System.out.print("Sensores: ");
        catalogo.getTodosSensores().forEach(s -> System.out.print(s.getCodigo() + "  "));
        System.out.println();
        String codigo = lerTexto("Código do sensor: ");
        try {
            Sensor sensor = catalogo.buscarSensor(codigo);
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
        int opcao = lerInteiro("Opção: ");
        try {
            switch (opcao) {
                case 1 -> dados.setPopulacaoEstimada(lerInteiro("Nova população estimada: "));
                case 2 -> dados.setDomiciliosEmAreaDeRisco(lerInteiro("Novos domicílios em área de risco: "));
                case 3 -> alterarLimiar(dados);
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
        System.out.println("\n--- STATUS COMPLETO ---");
        System.out.println("Território: " + catalogo.getDadosTerritorio());
        System.out.println("\nPontos monitorados:");
        catalogo.getPontos().forEach(p -> {
            p.verificarStatus();
            System.out.println("  " + p);
            p.getSensores().forEach(s -> System.out.println("     - " + s));
        });
        System.out.println("\nLimiares em uso:");
        for (Map.Entry<TipoSensor, Limiar> e : servico.getConfiguracao().getLimiares().entrySet()) {
            System.out.printf("  %-18s %s (%s)%n", e.getKey().getDescricao(), e.getValue(), e.getKey().getUnidade());
        }
        System.out.println("\nBanco: " + servico.contarMedicoes() + " medição(ões) e "
                + servico.contarAlertas() + " alerta(s) gravados.");
        // TODO (equipe): montar este relatório com um RelatorioBuilder (padrão Builder) se quiserem o 3º padrão.
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
