# O que já está pronto e o que falta

Situação em **19/11/2026**: compila, os **30 testes** passam e o menu foi executado de ponta a ponta numa cópia limpa
do projeto (empacotar → leitura → gravação → previsão → alerta → alerta repetido → exportação CSV → alteração de
limiar → reinício com dados e limiares preservados).
A base já tem 1 commit em `origin` = https://github.com/Senador2006/GS2_POO. **As melhorias abaixo ainda não foram
commitadas** (ver §2, item 3).

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
| Persistência | Medições, alertas e limiares alterados gravados (JPA) | ✅ |
| | Histórico por sensor e alertas no menu | ✅ opção 3 |
| | Sem SQL concatenado / recursos geridos | ✅ Spring Data + `try-with-resources` no CSV |
| | Dados sobrevivem ao reiniciar | ✅ H2 em arquivo |
| Padrão criacional | Pelo menos 1 padrão aplicado de verdade | ✅ Factory + Singleton + Builder |
| | Justificativa no README | ✅ seção "Padrões criacionais" do `README.md` da raiz |
| Testes | ≥ 5 testes AAA | ✅ 30 |
| | ≥ 1 `assertThrows` | ✅ vários |
| | ≥ 1 Mockito no repository | ✅ `ServicoMonitoramentoTest` |
| | Reflexão crítica no README | ✅ seção "Reflexão crítica" (equipe deve revisar, ver §2) |
| Entrega | README com público beneficiado, decisões, reflexão, uso de IA e divisão de tarefas | ✅ preenchido (equipe deve revisar, ver §2) |
| | Commits de **todos** os integrantes, com mensagens claras | ❌ **falta** (só a equipe pode fazer) |
| Bônus (+0,5) | CSV **ou** simulação de 24h **ou** dados reais (INMET) | ✅ CSV (menu 3 → 3) |

## 2. Obrigatório antes de entregar

1. ✅ **`README.md` da raiz preenchido** com público beneficiado, decisões da equipe, justificativa dos padrões,
   reflexão crítica, uso de IA e divisão de tarefas. **Revisar antes de entregar:**
   * **Divisão de tarefas**: a tabela segue a sugestão do §4 na ordem dos integrantes (Thiago = A, Pedro = B,
     Lucas = C). Trocar se a divisão real for outra.
   * **Uso de IA → "Como revisamos"**: confirmar que descreve o que cada um realmente fez (ou ajustar).
   * **Reflexão crítica**: cada integrante ler e reescrever com as próprias palavras o trecho da sua área. É a parte
     que o professor mais usa para ver se a equipe entendeu o problema.
2. **Cada integrante ler e saber explicar o código**, incluindo as classes novas (`LimiarSalvo`, `LimiarRepository`,
   `RelatorioBuilder`, `ExportadorCsv`) e o filtro de alerta repetido em `ServicoMonitoramento`.
   Sugestão: cada um apresenta a parte do outro em voz alta antes da correção.
3. **Commits de todos os integrantes.** Sugestão: cada um commitar a parte que ficou sob sua responsabilidade no §4,
   com mensagens claras, por exemplo:
   * `feat: exporta histórico de sensor em CSV` (`ExportadorCsv`, menu 3 → 3)
   * `feat: grava limiares alterados no banco` (`LimiarSalvo`, `LimiarRepository`, menu 4 → 4)
   * `feat: não grava alerta repetido sem leitura nova` (`ServicoMonitoramento`, `AnalisePrevisao`)
   * `feat: monta status completo com RelatorioBuilder`
   * `feat: simula chuva em episódios no pluviômetro`
   * `test: adiciona @DataJpaTest dos repositories`, `build: carrega Mockito como agente no surefire`
   * `docs: preenche README e atualiza docs`
4. ✅ **"Rodar seguindo o README" numa máquina limpa** testado numa cópia sem `target/` e `data/`
   (`.\mvnw.cmd package -DskipTests` → `java -jar ...`). Repetir depois do último commit com um **clone novo** do
   GitHub, para garantir que nenhum arquivo ficou de fora. Se não rodar, são **−3 pontos**.

## 3. Melhorias recomendadas (não obrigatórias, mas fortalecem a nota)

| Prioridade | Melhoria | Situação |
|---|---|---|
| Alta | **Bônus: exportar CSV** do histórico consultado | ✅ `servico/ExportadorCsv` + menu 3 → 3, com `try-with-resources` e `Files.newBufferedWriter` (RN11) |
| Alta | Persistir os **limiares alterados** | ✅ entidade `LimiarSalvo` (tabela `LIMIAR`) + `LimiarRepository`; carregados ao iniciar; menu 4 → 4 restaura os padrões (RN09) |
| Média | **Builder** `RelatorioBuilder` para a opção 5, como 3º padrão | ✅ `servico/RelatorioBuilder`, justificado no README |
| Média | Evitar **alerta duplicado** sem novas leituras | ✅ `ServicoMonitoramento` devolve o alerta anterior sem gravar (`AnalisePrevisao.alertaRepetido`, RN08) |
| Média | Teste de integração com `@DataJpaTest` | ✅ `RepositoriosJpaTest` (3 testes, H2 em memória) |
| Baixa | Alternativa de bônus: **simulação de 24h** usando o construtor com `Clock` do serviço | ⏭️ não feito: o bônus já está coberto pelo CSV e só um conta. Fica como evolução opcional |
| Baixa | Tirar o aviso do Mockito ("self-attaching") | ✅ `maven-dependency-plugin` (`properties`) + `argLine` no `maven-surefire-plugin` |
| Baixa | Episódios de chuva em sequência | ✅ `SensorPluviometrico.gerarValor` (RN02) + `SensorPluviometricoTest` |

## 4. Sugestão de divisão entre os 3 integrantes

Seguindo a divisão sugerida pelo próprio enunciado (é a mesma usada na tabela do README; ajustem se for diferente):

| Integrante | Responsável por (estudar, explicar e evoluir) | Evolução já implementada que deve saber explicar |
|---|---|---|
| A: domínio e exceções | `dominio/` (exceto previsões), `excecao/`, validações | `RelatorioBuilder` e episódios de chuva no `SensorPluviometrico` |
| B: persistência | `persistencia/`, entidades `Medicao`/`Alerta`/`LimiarSalvo`, `application.properties`, H2 | Limiares gravados + `RepositoriosJpaTest` + `ExportadorCsv` (bônus) |
| C: previsões, alertas e testes | `EstrategiaPrevisao` e filhas, `FabricaPrevisao`, `ConfiguracaoSistema`, `ServicoMonitoramento`, `src/test` | Filtro de alertas repetidos + testes novos do `ServicoMonitoramentoTest` + Mockito no `pom.xml` |
| Todos | README (cada um revisa a parte da reflexão sobre sua área) | |

## 5. Checklist final

- [x] `.\mvnw.cmd test` verde (30 testes)
- [x] `java -jar` roda e o menu responde às 5 opções
- [x] Reiniciar o programa e ver o histórico (e os limiares alterados) ainda lá
- [x] README: público, decisões, padrões justificados, reflexão, uso de IA, divisão de tarefas, como rodar
- [ ] README revisado pela equipe (divisão de tarefas, "Como revisamos", reflexão com as próprias palavras)
- [x] Nenhum dado pessoal real no código ou no banco
- [ ] Commits de todos os integrantes no GitHub
- [ ] Clone novo do GitHub rodando seguindo o README
- [x] Pastas `data/`, `exportacoes/` e `target/` **fora** do repositório (já estão no `.gitignore`)
