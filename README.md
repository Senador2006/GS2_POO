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
Os dados continuam lá ao reiniciar. Para zerar, apague a pasta `data`.
* Código de acesso para alterar limiares (menu 4 → 3): `2026` (em `src/main/resources/application.properties`).
* Acentos quebrados no PowerShell? Rode `chcp 65001` antes.

## Público beneficiado

TODO (equipe): quem sofre o problema e qual decisão o sistema apoia.

## Decisões da equipe

TODO (equipe): recorte escolhido, Spring Boot + JPA, H2 em arquivo, janelas de previsão, limiares.

## Padrões criacionais

TODO (equipe): justificar **Factory** (`FabricaPrevisao`) e **Singleton** (`ConfiguracaoSistema`): por que ali e não em outro lugar.

## Reflexão crítica

TODO (equipe, meia página):

* Quando o sistema deve se calar?
* O que os dados não representam?
* Por que o alerta apoia a decisão da pessoa, em vez de decidir por ela?

## Uso de IA

TODO (equipe): onde a IA foi usada e como o código foi revisado.

## Divisão de tarefas

TODO (equipe).

## Documentação interna

Explicação do código, regras de negócio e pendências estão em [`docs/`](docs/README.md).
