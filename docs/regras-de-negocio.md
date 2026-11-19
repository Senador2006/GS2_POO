# Regras de negócio

## Contexto

**Problema:** bairros às margens de córregos e em encostas sofrem com alagamentos e deslizamentos em chuvas fortes.
**Pergunta que o sistema ajuda a responder:** *"a chuva dos próximos períodos vai atingir os pontos vulneráveis do
território, e o que podemos fazer hoje para reduzir o impacto?"*
**Quem usa:** associação de moradores, Defesa Civil municipal ou subprefeitura.
**Decisão que o sistema apoia:** avisar moradores, preparar equipes, liberar bueiros, indicar rotas e abrigos.
O sistema **não decide** nada sozinho.

## RN01: Território e pontos monitorados

* O território é descrito por **região e equipamento**, nunca por pessoas (sem nomes, CPF ou endereços).
* Território de exemplo: *Bacia do Córrego Esperança*, população estimada 18.500, 420 domicílios em área de risco.
* Pontos (todos fictícios):

| Ponto | Nome | Região | Sensores |
|---|---|---|---|
| PT-01 | Margem do córrego, trecho baixo | Zona Leste | PLU-01, NIV-01 |
| PT-02 | Encosta da vila alta | Zona Norte | PLU-02, UMI-01 |
| PT-03 | Passagem subterrânea central | Centro | PLU-03, NIV-02 |

* População e domicílios em risco **não podem ser negativos** (`DadosInvalidosException`).
* Nome do território, do ponto e da região **não podem ser vazios**.

## RN02: Sensores e faixa física válida

| Tipo | Unidade | Faixa física aceita | Valor simulado |
|---|---|---|---|
| Pluviômetro | mm/h | 0 a 150 | 70% das vezes 0–15; 30% das vezes 15–85 |
| Nível do córrego | m | 0 a 10 | 0,5 a 5,0 |
| Umidade do solo | % | 0 a 100 | 30 a 100 |

* A **faixa física** separa um valor *impossível* (defeito) de um valor *perigoso* (risco). Fora da faixa, a medição é
  **rejeitada** e **não é gravada**.
* Simulação de falhas: em cada leitura há **5%** de chance de o sensor não responder (`FonteIndisponivelException`) e
  **3%** de chance de vir um valor negativo corrompido (`DadosInvalidosException`).
* Um sensor que falha numa leitura volta a ser tentado na próxima. `estaFuncionando()` reflete a **última** tentativa.

## RN03: Status de um ponto

| Sensores que responderam na última leitura | Status |
|---|---|
| todos | OPERANDO |
| alguns | PARCIAL |
| nenhum (ou ponto sem sensor) | INOPERANTE |
| ainda não verificado | NAO_VERIFICADO |

## RN04: Gravação do histórico

* **Toda medição válida** é gravada na tabela `MEDICAO` (sensor, tipo, valor, data/hora).
* **Todo alerta emitido** é gravado na tabela `ALERTA`.
* O banco H2 fica em arquivo, então o histórico **sobrevive ao reiniciar** o programa.
* O histórico de um sensor é lido em ordem cronológica (mais antigo primeiro).

## RN05: Previsões

As duas estratégias rodam sobre o histórico **do banco** daquele sensor.

| Estratégia | Fórmula | Mínimo de leituras | Para que serve |
|---|---|---|---|
| Média móvel | média das últimas **N = 5** leituras | 5 | suaviza picos isolados e mostra o nível "de fundo" |
| Comparação com período anterior | `recente + (recente − anterior)`, em que *recente* é a média das últimas **3** e *anterior* a média das 3 antes delas; nunca abaixo de 0 | 6 | reage mais rápido a uma tendência de subida |

* Cada resultado traz **valor previsto + o que ele significa** (frase explicando tendência, variação % e unidade).
* As janelas (5 e 3) vêm do `ConfiguracaoSistema`.

## RN06: Quando o sistema se cala

* Se o sensor tiver **menos leituras que o mínimo da estratégia**, a previsão é marcada como **não confiável**,
  o valor fica vazio (NaN) e aparece a mensagem *"Histórico insuficiente (x de y leituras)"*.
* Previsão não confiável **nunca gera alerta**. É melhor não avisar do que gerar alarme falso que desgasta a confiança
  da comunidade.

## RN07: Limiares e níveis de alerta

Limiares padrão (`ConfiguracaoSistema.restaurarPadroes()`):

| Tipo | ATENÇÃO a partir de | ALERTA a partir de | CRÍTICO a partir de |
|---|---|---|---|
| Pluviômetro (mm/h) | 20 | 40 | 60 |
| Nível do córrego (m) | 2,5 | 3,5 | 4,5 |
| Umidade do solo (%) | 70 | 85 | 95 |

* Um limiar só é válido se for **não negativo** e **estritamente crescente** (atenção < alerta < crítico).
* A classificação usa `>=`: exatamente 60 mm/h já é CRÍTICO.
* Abaixo do limite de ATENÇÃO, nenhum alerta é emitido.

## RN08: Emissão e explicação do alerta

* Para cada sensor e cada estratégia: se a previsão é confiável e atinge algum limiar, o sistema emite e grava um alerta
  com o **nível mais alto** atingido.
* Todo alerta explica: **qual sensor**, **valor previsto**, **qual estratégia**, **qual limite foi estourado** e a
  **ação sugerida**:

| Nível | Ação sugerida |
|---|---|
| ATENÇÃO | Acompanhar as próximas leituras e avisar os moradores dos pontos vulneráveis |
| ALERTA | Preparar a equipe de apoio, liberar bueiros e indicar rotas seguras |
| CRÍTICO | Acionar a Defesa Civil e orientar a saída preventiva das áreas de risco |

* Como as duas estratégias rodam, um mesmo sensor pode gerar **até 2 alertas** por execução (um por estratégia).
  Isso é intencional: mostra quando as estratégias concordam ou discordam.

## RN09: Alteração de limiares (código de acesso)

* Só é possível alterar um limiar informando o **código de acesso** do território (`territorio.codigo-acesso`
  no `application.properties`, padrão `2026`).
* Código errado: `DadosInvalidosException` e nada é alterado. O código é conferido **antes** de pedir os novos valores.
* O código não tem getter: nenhuma parte do sistema consegue lê-lo, só conferi-lo.
* ⚠️ Hoje a alteração vale **só até fechar o programa** (o Singleton vive em memória). Ver pendências.

## RN10: Tratamento de erros no menu

* Nenhum `catch` vazio: todo erro capturado mostra ao usuário **o que aconteceu** e **o que foi feito**
  (ex.: "DESCARTADA Dados inválidos: medição de pluviômetro fora da faixa: -3,20 mm/h ... (não gravada)").
* Entrada não numérica no menu é pedida de novo, sem derrubar o programa.
* Falha de banco (`DataAccessException`) é capturada no laço principal com mensagem própria.
