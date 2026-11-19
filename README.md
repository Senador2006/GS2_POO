# Territory Predictive Insights: chuva e risco de alagamento

Global Solution 2º/2026, Programação Orientada a Objetos (Turma de Fevereiro). Tema: CitizenTech.

Plataforma de console que lê sensores de chuva, nível de córrego e umidade do solo, grava o histórico em banco,
prevê as próximas leituras com duas estratégias e emite alertas (ATENÇÃO, ALERTA, CRÍTICO) explicando o porquê.

## Integrantes

| Nome         | RM     |
| ------------ | ------ |
| Thiago Sakai | 563448 |
| Pedro Mitsu  | 561710 |
| Lucas Eiki   | 561607 |

## Como rodar

Pré-requisito: **JDK 17 ou superior** (o Maven é baixado automaticamente pelo `mvnw`).

```powershell
.\mvnw.cmd test
```

```powershell
.\mvnw.cmd package -DskipTests
```

```powershell
java -jar target\territory-predictive-insights-fev-1.0.0.jar
```

* No Linux/macOS, use `./mvnw` no lugar de `.\mvnw.cmd`.
* Banco: **H2 em arquivo**, criado automaticamente em `./data/territorio.mv.db`. Não é preciso instalar nada.
Os dados continuam lá ao reiniciar (medições, alertas e limiares alterados). Para zerar, apague a pasta `data`.
* Código de acesso para alterar ou restaurar limiares (menu 4 → 3 ou 4 → 4): `2026` (em `src/main/resources/application.properties`).
* Exportação CSV (menu 3 → 3): o arquivo é gravado em `./exportacoes/<sensor>_<data-hora>.csv`, com `;` e vírgula decimal (abre direto no Excel).
* As previsões precisam de pelo menos 5 leituras (média móvel) e 6 (comparação): rode a opção 1 algumas vezes antes da opção 2.
* Acentos quebrados no PowerShell? Rode `chcp 65001` antes.

## Público beneficiado

Moradores de bairros às margens de córregos e em encostas, que sofrem com alagamentos e deslizamentos em chuvas
fortes. No território de exemplo (fictício), a *Bacia do Córrego Esperança*, são cerca de 18.500 pessoas e 420
domicílios em área de risco, distribuídos em três pontos vulneráveis: a margem baixa do córrego, uma encosta e uma
passagem subterrânea.

Quem **opera** o sistema é a associação de moradores, a Defesa Civil municipal ou a subprefeitura. A pergunta que ele
ajuda a responder é: *"a chuva dos próximos períodos vai atingir os pontos vulneráveis, e o que podemos fazer hoje para
reduzir o impacto?"*. As decisões apoiadas são concretas e escalonadas pelo nível do alerta: avisar os moradores
(ATENÇÃO), preparar a equipe de apoio, liberar bueiros e indicar rotas seguras (ALERTA) e acionar a Defesa Civil para
a saída preventiva (CRÍTICO).

## Decisões da equipe

| Decisão | Escolha | Por quê |
|---|---|---|
| Recorte | Clima local: chuva intensa e risco de alagamento | Problema real e recorrente nas cidades brasileiras, com três sensores naturais (chuva, nível do córrego, umidade do solo) |
| Território | Descrito por região e equipamento, nunca por pessoas | Nenhum dado pessoal (nome, CPF, endereço) entra no código ou no banco |
| Persistência | Spring Boot + Spring Data JPA | O Spring gera o SQL com parâmetros (sem concatenação) e abre/fecha conexões e transações; a equipe já usava essa stack |
| Banco | H2 em arquivo (`./data`) | Roda na correção sem instalar nada e, ao contrário do H2 em memória, mantém o histórico ao reiniciar |
| Faixa física × limiar | Duas regras separadas | Um valor fora da faixa física (chuva negativa, umidade > 100%) é **defeito** e é descartado; um valor alto dentro da faixa é **risco** e pode gerar alerta |
| Janelas de previsão | Média móvel de 5 leituras; comparação das 3 últimas com as 3 anteriores | 5 leituras suavizam um pico isolado sem esconder uma chuva que se mantém; 3 contra 3 reage mais rápido a uma subida. Rodar as duas lado a lado mostra quando concordam |
| Limiares padrão | Chuva 20 / 40 / 60 mm/h; córrego 2,5 / 3,5 / 4,5 m; solo 70 / 85 / 95 % | Faixas próximas às usadas em boletins de chuva forte (acima de 50 mm/h já é chuva muito forte). São configuráveis pelo menu, protegidas por código de acesso e gravadas no banco |
| Alerta repetido | Rodar a previsão de novo sem leituras novas não grava outro alerta igual | O mesmo histórico dá o mesmo resultado; gravar cópias inflaria o histórico de alertas e passaria a impressão de que o risco aumentou |
| Simulação | Sensores com `Random`, 5% de falha de comunicação e 3% de leitura corrompida; chuva forte em episódios de algumas leituras seguidas | Exercita as exceções e gera tendências que as previsões conseguem detectar |
| Bônus | Exportação do histórico de um sensor em CSV | Permite levar os dados para uma planilha ou para quem vai decidir fora do sistema |

## Padrões criacionais

**Factory (**`FabricaPrevisao`**).** Escolher a estratégia de previsão é a única decisão do sistema do tipo "qual classe
instanciar". Sem a fábrica, o serviço teria `if (tipo == MEDIA_MOVEL) new ... else ...` e precisaria conhecer os
parâmetros de cada estratégia (janela de 5? de 3?). A fábrica guarda um `EnumMap<TipoEstrategia, Supplier<...>>` com a
"receita" de cada uma: o serviço só pede `criar(tipo)` ou `criarTodas()`, sem `if`, `switch` nem `instanceof`, e uma
estratégia nova entra com uma linha no construtor da fábrica. Não usamos Factory para `Medicao` ou `Alerta` porque
eles têm um único tipo e o construtor já valida tudo: ali a fábrica seria só uma camada a mais.

**Singleton (**`ConfiguracaoSistema`**).** Os limiares precisam ser **os mesmos** para quem emite o alerta (serviço), quem
os mostra (menu, opção 5) e quem os altera (opção 4). Duas instâncias poderiam comparar previsões com limiares
diferentes sem ninguém perceber. O construtor é privado, a instância sai de `getInstancia()` e o Spring a recebe por
`BeansConfig`, sem criar um segundo objeto escondido. O custo do Singleton é o estado global: por isso os testes chamam
`restaurarPadroes()` antes de cada caso. Não usamos Singleton nos repositories ou no serviço porque o Spring já garante
uma instância deles e eles não guardam estado próprio.

**Builder (**`RelatorioBuilder`**).** O relatório da opção 5 tem várias seções opcionais (território, pontos, limiares,
totais do banco, últimos alertas). Um construtor com todas elas como parâmetro seria ilegível e obrigaria a passar
`null` no que não se quer mostrar. Com o builder, quem chama encadeia só as seções que quer
(`.comTerritorio(...).comLimiares(...).construir()`), na ordem que quer, e a formatação fica fora do menu.
`construir()` recusa um relatório sem nenhuma seção.

## Reflexão crítica

**Quando o sistema deve se calar?** Quando não tem dados suficientes para afirmar alguma coisa. Com menos de 5
leituras (média móvel) ou 6 (comparação), a previsão é marcada como não confiável, o valor fica vazio e nenhum alerta é
emitido. Também se cala diante de leituras impossíveis, que são descartadas em vez de entrar na média, e não repete
um alerta quando não chegou nenhuma leitura nova. Preferimos o silêncio a um alarme falso: cada alarme falso gasta a
confiança da comunidade, e um aviso ignorado no dia em que é verdadeiro custa muito mais. Ainda assim, 5 leituras é
pouco: com leituras de hora em hora, é uma janela de 5 horas, curta para capturar o efeito acumulado de vários dias de
chuva sobre o solo.

**O que os dados não representam?** Os dados são simulados, não medidos. Há um pluviômetro por ponto, quando a chuva
varia muito de uma rua para outra. O sistema olha a intensidade instantânea (mm/h), não a chuva acumulada das últimas
24 ou 72 horas, que é o que mais pesa em deslizamentos. Não há informação sobre bueiros entupidos, obras, maré ou
chuva rio acima, e as leituras não têm intervalo fixo: elas acontecem quando alguém escolhe a opção 1. Os limiares
são valores de referência e precisariam ser calibrados com o histórico real de cada ponto e com quem conhece o
território.

**Por que o alerta apoia a decisão em vez de decidir?** Porque quem decide tem contexto que o sistema não tem: sabe
se a rota de fuga está alagada, se há idosos ou pessoas acamadas na área, se o abrigo está disponível, se o córrego
costuma transbordar naquele ponto. Uma evacuação tem custo alto (gente sai de casa, deixa trabalho e pertences), e um
falso positivo automático cobraria esse custo sem necessidade. Por isso todo alerta **explica** o porquê (sensor,
valor previsto, estratégia, limite ultrapassado) e **sugere** uma ação, deixando a decisão final com a pessoa
responsável, que pode confirmar, adiar ou descartar com base no que vê no território.

## Uso de IA

Utilizamos a IA principalmente na estruturação base da arquitetura e como revisão de codigo, para a projeção arquitetural e analise de requisitos usamos o Claude Code no modelo Opus 5.5 Medium.

Para revisão e correção de codigo, utilizamos o cursor para verificar e analisar possiveis erros de clean code cometidos.  


Como revisamos:

* Rodamos `.\mvnw.cmd test` (30 testes) e conferimos que cada teste testa o que o nome diz.
* Executamos o menu de ponta a ponta numa cópia limpa do projeto: leituras, previsões, alerta repetido, exportação
CSV, código de acesso errado, alteração de limiar e reinício do programa com os dados preservados.
* Lemos cada classe e conferimos se as regras implementadas batem com as de `docs/regras-de-negocio.md`
(faixas, limiares, mínimo de leituras, quando alertar).
* Ajustamos o texto das decisões e da reflexão ao que a equipe de fato pensa sobre o problema.

## Divisão de tarefas

| Integrante | Responsável por |
|---|---|
| Thiago Sakai | Domínio e exceções (`dominio/`, `excecao/`, validações), `RelatorioBuilder` e simulação de chuva em episódios |
| Pedro Mitsu | Persistência (`persistencia/`, entidades, `application.properties`, H2), limiares gravados no banco, teste `@DataJpaTest` e exportação CSV |
| Lucas Eiki | Previsões, alertas e testes (`EstrategiaPrevisao` e filhas, `FabricaPrevisao`, `ConfiguracaoSistema`, `ServicoMonitoramento`, `src/test`), filtro de alertas repetidos |
| Todos | README e reflexão crítica (cada um sobre a sua área) |

## Documentação interna

Explicação do código, regras de negócio e pendências estão em [`docs/`](docs/README.md).
