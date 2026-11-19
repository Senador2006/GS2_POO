# Documentação interna da equipe

Material de apoio para a Global Solution 2º/2026 de POO, **Territory Predictive Insights (Turma de Fevereiro)**.
Esta pasta é para a equipe estudar e se organizar. O `README.md` da raiz é o que vai para a correção.

| Arquivo | Para que serve |
|---|---|
| [arquitetura-e-codigo.md](arquitetura-e-codigo.md) | Como o código está organizado, o que cada classe faz e o fluxo de uma leitura até virar um alerta |
| [regras-de-negocio.md](regras-de-negocio.md) | As regras que o sistema aplica: faixas válidas, limiares, quando prever, quando alertar, quando se calar |
| [pendencias.md](pendencias.md) | O que já está pronto, o que falta fazer, uma sugestão de divisão entre os 3 integrantes e um checklist de entrega |

## Resumo das decisões já tomadas

| Decisão | Escolha | Por quê |
|---|---|---|
| Recorte | 🌊 **Clima local**: chuva intensa e risco de alagamento | É o exemplo mais usado no enunciado (chuva negativa, INMET) e tem 3 sensores naturais |
| Persistência | **Spring Boot 4.1.1 + Spring Data JPA** | Mesma stack do CS3_POO, que a equipe já conhece |
| Banco | **H2 em arquivo** (`./data/territorio.mv.db`) | Roda na correção sem instalar nada e, ao contrário do H2 em memória, **mantém os dados ao reiniciar** (medições, alertas e limiares alterados) |
| Padrões criacionais | **Factory** (`FabricaPrevisao`) + **Singleton** (`ConfiguracaoSistema`) + **Builder** (`RelatorioBuilder`) | O enunciado pede pelo menos 1; os três resolvem problemas reais do código (ver arquitetura §5) |
| Bônus | **Exportação CSV** do histórico de um sensor (menu 3 → 3) | O mais simples dos três bônus e útil para levar os dados a uma planilha |
| Java | 17 no `pom.xml` | Compatível com qualquer JDK 17+ dos integrantes (foi testado com o JDK 25) |

> ⚠️ Lembrete do enunciado: na correção **qualquer integrante pode ser chamado para explicar qualquer trecho**.
> Esta base foi gerada com apoio de IA. Leiam, rodem e entendam cada classe, e declarem isso na seção "Uso de IA" do README.
