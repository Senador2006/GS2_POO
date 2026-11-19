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
| Pluviômetro | mm/h | 0 a 150 | fora de episódio 0–15; em episódio de chuva forte 15–85 (cerca de 1/3 das leituras) |
| Nível do córrego | m | 0 a 10 | 0,5 a 5,0 |
| Umidade do solo | % | 0 a 100 | 30 a 100 |

* A **faixa física** separa um valor *impossível* (defeito) de um valor *perigoso* (risco). Fora da faixa, a medição é
  **rejeitada** e **não é gravada**.
* Simulação de falhas: em cada leitura há **5%** de chance de o sensor não responder (`FonteIndisponivelException`) e
  **3%** de chance de vir um valor negativo corrompido (`DadosInvalidosException`).
* Um sensor que falha numa leitura volta a ser tentado na próxima. `estaFuncionando()` reflete a **última** tentativa.
* **Episódios de chuva (pluviômetro):** a cada leitura sem chuva forte há **15%** de chance de começar uma pancada,
  que começa entre 15 e 40 mm/h e a cada leitura varia em torno da anterior (−7 a +13 mm/h, limitada a 15–85).
  A cada leitura há **30%** de chance de a pancada acabar (dura em média ~3 leituras). Assim as previsões encontram
  subidas e quedas reais, em vez de valores sorteados sem relação entre si.

## RN03: Status de um ponto

| Sensores que responderam na última leitura | Status |
|---|---|
| todos | OPERANDO |
| alguns | PARCIAL |
| nenhum (ou ponto sem sensor) | INOPERANTE |
| ainda não verificado | NAO_VERIFICADO |

## RN04: Gravação do histórico

* **Toda medição válida** é gravada na tabela `MEDICAO` (sensor, tipo, valor, data/hora).
* **Todo alerta emitido** é gravado na tabela `ALERTA` (exceto o alerta repetido, ver RN08).
* **Todo limiar alterado** é gravado na tabela `LIMIAR` (uma linha por tipo de sensor; ver RN09).
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
* **Alerta repetido não é gravado de novo.** Se o último alerta do mesmo sensor e da mesma estratégia tem o
  **mesmo nível** e **nenhuma medição** chegou depois dele, o menu mostra esse alerta como *"mantido: já emitido em
  ..., sem leituras novas"* e não grava outra linha em `ALERTA`. Basta uma leitura nova (opção 1) ou um nível diferente
  para um alerta novo ser gravado.

## RN09: Alteração de limiares (código de acesso)

* Só é possível alterar um limiar informando o **código de acesso** do território (`territorio.codigo-acesso`
  no `application.properties`, padrão `2026`).
* Código errado: `DadosInvalidosException` e nada é alterado. O código é conferido **antes** de pedir os novos valores.
* O código não tem getter: nenhuma parte do sistema consegue lê-lo, só conferi-lo.
* O limiar alterado é aplicado no `ConfiguracaoSistema` **e** gravado na tabela `LIMIAR`. Ao iniciar, o programa
  carrega os limiares gravados (*"1 limiar(es) alterado(s) em execuções anteriores carregado(s) do banco"*);
  tipos sem linha gravada usam o padrão da RN07.
* Se alguma linha de `LIMIAR` estiver inválida (editada à mão no banco), **nenhuma** é aplicada e o programa avisa
  que está usando os padrões.
* Menu 4 → 4 **restaura os limiares padrão** (também exige o código): apaga a tabela `LIMIAR` e volta à RN07.

## RN10: Tratamento de erros no menu

* Nenhum `catch` vazio: todo erro capturado mostra ao usuário **o que aconteceu** e **o que foi feito**
  (ex.: "DESCARTADA Dados inválidos: medição de pluviômetro fora da faixa: -3,20 mm/h ... (não gravada)").
* Entrada não numérica no menu é pedida de novo, sem derrubar o programa.
* Falha de banco (`DataAccessException`) é capturada no laço principal com mensagem própria.
* Falha ao gravar o CSV (`IOException`) é capturada na opção 3 → 3 com mensagem própria.

## RN11: Exportação do histórico em CSV (bônus)

* Menu 3 → 3 exporta **todas as medições de um sensor** (em ordem cronológica) para
  `./exportacoes/<sensor>_<aaaaMMdd-HHmmss>.csv`. A pasta é configurável em `territorio.pasta-exportacao`.
* Formato: cabeçalho `data_hora;codigo_sensor;tipo_sensor;valor;unidade`, separador `;` e vírgula decimal
  (abre direto no Excel em português). Exemplo: `19/11/2026 13:11:13;PLU-01;PLUVIOMETRO;28,68;mm/h`.
* Sensor sem medições: nada é exportado e nenhum arquivo é criado (`DadosInvalidosException`).
* A pasta `exportacoes/` está no `.gitignore`.
