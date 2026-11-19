# O que já está pronto e o que falta

Situação da base em **19/11/2026**: compila, os 18 testes passam e o menu foi executado de ponta a ponta
(leitura → gravação → previsão → alerta → histórico → reinício com dados preservados).
**Nada foi commitado ainda.** O repositório local já aponta para `origin` = https://github.com/Senador2006/GS2_POO.

## 1. Requisitos do enunciado × situação

| Critério (2,0 pts cada) | Item | Situação |
|---|---|---|
| Domínio e polimorfismo | `ComponenteTerritorial` abstrata + `verificarStatus()` + filha concreta | ✅ `PontoMonitoramento` |
| | Interface `Sensor` com 3 implementações | ✅ pluviômetro, nível do córrego, umidade do solo |
| | `DadosTerritorio` com atributos privados e código de acesso para limiares | ✅ |
| | `EstrategiaPrevisao` + 2 filhas sobrescrevendo `prever()` com `super` | ✅ |
| | Escolha da estratégia sem `if tipo ==` / `instanceof` | ✅ `FabricaPrevisao` com `EnumMap` |
| Exceções | `DadosInvalidosException` e `FonteIndisponivelException` lançadas em setters e serviços | ✅ |
| | Captura no menu com mensagem clara, sem `catch` vazio | ✅ |
| Persistência | Medições e alertas gravados (JPA) | ✅ |
| | Histórico por sensor e alertas no menu | ✅ opção 3 |
| | Sem SQL concatenado / recursos geridos | ✅ Spring Data |
| | Dados sobrevivem ao reiniciar | ✅ H2 em arquivo |
| Padrão criacional | Pelo menos 1 padrão aplicado de verdade | ✅ Factory + Singleton |
| | **Justificativa no README** | ❌ **falta escrever no README da raiz** (rascunho em [arquitetura-e-codigo.md §5](arquitetura-e-codigo.md#5-por-que-cada-padrão-está-onde-está-base-para-o-readme)) |
| Testes | ≥ 5 testes AAA | ✅ 18 |
| | ≥ 1 `assertThrows` | ✅ vários |
| | ≥ 1 Mockito no repository | ✅ `ServicoMonitoramentoTest` |
| | **Reflexão crítica no README** | ❌ **falta** |
| Entrega | README com público beneficiado, decisões, reflexão, uso de IA e divisão de tarefas | ❌ **falta preencher** (o `README.md` da raiz tem os títulos e TODOs) |
| | Commits de **todos** os integrantes, com mensagens claras | ❌ **falta** |
| Bônus (+0,5) | CSV **ou** simulação de 24h **ou** dados reais (INMET) | ❌ opcional |

## 2. Obrigatório antes de entregar

1. **Preencher o `README.md` da raiz**, com as seções que o enunciado exige:
   * **Público beneficiado**: quem sofre o problema e qual decisão o sistema apoia (base em [regras-de-negocio.md](regras-de-negocio.md#contexto)).
   * **Decisões da equipe**: recorte, JPA + H2 em arquivo, janelas 5/3, limiares escolhidos e por quê.
   * **Justificativa dos padrões**: por que Factory e Singleton *ali* e não em outro lugar.
   * **Reflexão crítica (meia página)**, respondendo:
     * *Quando o sistema deve se calar?* (já implementado: menos de 5/6 leituras = não prevê; discutam se 5 é pouco)
     * *O que os dados não representam?* (1 pluviômetro por ponto, dados simulados, horário sem chuva acumulada, etc.)
     * *Por que o alerta apoia a decisão em vez de decidir?* (contexto local, custo de evacuação, falsos positivos)
   * **Uso de IA**: onde usaram (esta base foi gerada com apoio de IA) e **como revisaram**.
   * **Divisão de tarefas**: quem fez o quê.
2. **Cada integrante ler e saber explicar o código.** Sugestão: cada um apresenta a parte do outro em voz alta antes da correção.
3. **Commits de todos os integrantes.** Sugestão: fazer o primeiro commit da base e depois cada um commitar as próprias
   evoluções (abaixo) com mensagens claras, por exemplo `feat: exporta histórico em CSV`.
4. **Testar o "rodar seguindo o README" numa máquina limpa** (clone novo → `.\mvnw.cmd package` → `java -jar ...`).
   Se não rodar, são **−3 pontos**.

## 3. Melhorias recomendadas (não obrigatórias, mas fortalecem a nota)

| Prioridade | Melhoria | Onde mexer |
|---|---|---|
| Alta | **Bônus: exportar CSV** do histórico consultado (opção 3 → "exportar"). É o bônus mais simples. Usar `try-with-resources` com `BufferedWriter`/`Files.newBufferedWriter` | novo `servico/ExportadorCsv` + menu |
| Alta | Persistir os **limiares alterados** (hoje voltam ao padrão ao reiniciar): tabela `LIMIAR` + repository, ou `application.properties` | `ConfiguracaoSistema`, nova entidade |
| Média | **Builder** `RelatorioBuilder` para a opção 5 (status completo), como 3º padrão | menu, `TODO` em `statusCompleto()` |
| Média | Evitar **alerta duplicado** quando se roda a previsão várias vezes sem novas leituras (ex.: não gravar se o último alerta do sensor tem o mesmo nível e nenhuma medição nova) | `ServicoMonitoramento.avaliarAlerta` |
| Média | Teste de integração com `@DataJpaTest` provando que o repository grava e lê | `src/test` (dependência `spring-boot-starter-data-jpa-test`) |
| Baixa | Alternativa de bônus: **simulação de 24h** usando o construtor com `Clock` do serviço (avançar 1h por iteração) | `ServicoMonitoramento` já aceita `Clock` |
| Baixa | Tirar o aviso do Mockito ("self-attaching") configurando o agente no `maven-surefire-plugin` | `pom.xml` |
| Baixa | Ajustar as faixas simuladas para gerar episódios de chuva em sequência (mais realista que sorteio independente) | `SensorPluviometrico.gerarValor` |

## 4. Sugestão de divisão entre os 3 integrantes

Seguindo a divisão sugerida pelo próprio enunciado:

| Integrante | Responsável por (estudar, explicar e evoluir) | Tarefa de evolução sugerida |
|---|---|---|
| A: domínio e exceções | `dominio/` (exceto previsões), `excecao/`, validações | Builder `RelatorioBuilder` **ou** episódios de chuva mais realistas |
| B: persistência | `persistencia/`, entidades `Medicao`/`Alerta`, `application.properties`, H2 | Persistir limiares + teste `@DataJpaTest` + exportação CSV (bônus) |
| C: previsões, alertas e testes | `EstrategiaPrevisao` e filhas, `FabricaPrevisao`, `ConfiguracaoSistema`, `ServicoMonitoramento`, `src/test` | Evitar alertas duplicados + novos testes para isso |
| Todos | README (cada um escreve a parte da reflexão sobre sua área) | |

## 5. Checklist final

- [ ] `.\mvnw.cmd test` verde
- [ ] `java -jar` roda e o menu responde às 5 opções
- [ ] Reiniciar o programa e ver o histórico ainda lá
- [ ] README: público, decisões, padrões justificados, reflexão, uso de IA, divisão de tarefas, como rodar
- [ ] Nenhum dado pessoal real no código ou no banco
- [ ] Commits de todos os integrantes no GitHub
- [ ] Pasta `data/` e `target/` **fora** do repositório (já estão no `.gitignore`)
