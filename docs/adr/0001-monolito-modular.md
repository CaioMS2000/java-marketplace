# ADR-0001 — Monólito modular em vez de microsserviços

- **Status:** Aceito
- **Data:** 2026-06-26
- **Decisores:** Caio Marques Silva
- **Contexto:** backend de marketplace (projeto de portfólio, alvo pleno/sênior)

> Este é o primeiro ADR e estabelece o formato dos seguintes: contexto (forças em jogo), decisão, consequências (positivas e negativas), alternativas descartadas e gatilho de revisão. ADRs são imutáveis — se uma decisão mudar, escreve-se um novo ADR que supersede este, não se edita o antigo.

## Contexto

A decisão de arquitetura macro é a de maior peso para este projeto, e a tentação natural é partir para microsserviços. As forças em jogo apontam para o contrário:

- **Desenvolvedor único.** Microsserviços resolvem principalmente um problema *organizacional* — vários times deployando e escalando de forma independente. Esse problema não existe num projeto solo.
- **Núcleo transacional.** Criar pedido, reservar estoque e confirmar pagamento exigem consistência forte e invariantes sob concorrência. Isso é mais simples e mais seguro com transações locais ACID do que com transações distribuídas.
- **O valor do portfólio é profundidade e julgamento**, não largura nem buzzwords. O orçamento de esforço deve ir para o domínio, não para encanamento de infraestrutura distribuída.

## Decisão

Adotar um **monólito modular**:

- Um único artefato/deploy.
- Módulos = **bounded contexts**: `Identidade`, `Catálogo`, `Estoque`, `Pedidos`, `Pagamento`, `Notificação`.
- Fronteiras impostas com **Spring Modulith**, verificadas em teste: um módulo não pode importar os *internals* de outro, apenas sua API pública.
- Comunicação entre módulos: **síncrona** (via API pública) apenas quando a resposta é necessária para decidir agora; caso contrário, **eventos de domínio assíncronos** (in-process hoje, prontos para Kafka).
- **Arquitetura hexagonal + DDD tático aplicados seletivamente** ao núcleo (`Pedidos`, `Pagamento` e o pequeno núcleo rico de `Estoque`). Módulos de apoio permanecem simples/CRUD.

## Consequências

### Positivas

- Transação local ACID onde importa (criar pedido + reservar estoque numa única transação).
- Baixo overhead operacional: `docker compose up` sobe tudo; desenvolvimento local rápido.
- Fronteiras verificadas no build — o teste falha se um módulo violar o limite de outro. As fronteiras se documentam sozinhas.
- Comunicação por eventos mantém o grafo de dependências **acíclico** (o publicador não conhece os assinantes).
- Caminho de extração preservado: os eventos in-process já têm o formato de eventos Kafka; extrair um módulo para serviço próprio depois é uma mudança localizada.

### Negativas / tradeoffs

- Não é possível escalar módulos de forma independente; um deploy publica tudo.
- A disciplina de fronteira é imposta por ferramenta + teste, não pela rede física — exige manter essa disciplina (o Spring Modulith ajuda, mas não é a barreira intransponível que um processo separado seria).
- Troca o sinal "usei microsserviços" pelo sinal "soube *não* usar microsserviços" — aposta deliberada de que, para o público-alvo (revisor técnico sênior), o segundo é mais forte.

## Alternativas consideradas

**Microsserviços desde o início — rejeitado.** Resolve um problema organizacional ausente num projeto solo e paga todos os custos da distribuição (transação distribuída, falha de rede, consistência eventual, observabilidade distribuída, overhead operacional) sem colher os benefícios. Para um revisor experiente, costuma sinalizar over-engineering, não senioridade.

**Monólito tradicional sem módulos — rejeitado.** Sem fronteiras impostas, o sistema degenera em *big ball of mud*. Não demonstra raciocínio de bounded context, que é parte do sinal que o projeto quer emitir.

**Monólito modular — escolhido.** Combina a simplicidade operacional do monólito com fronteiras explícitas e verificáveis, e preserva o caminho para distribuição futura caso ela se justifique.

## Gatilho de revisão

Reconsiderar a extração de um módulo para serviço próprio quando — e somente quando — surgir pelo menos um destes:

- necessidade **real e medida** de escalar aquele módulo independentemente, sob tráfego real;
- crescimento de time a ponto de a coordenação num único deploy virar gargalo;
- requisito de isolamento (compliance, multi-tenancy) que justifique um processo separado.

Até lá, a fronteira de módulo é suficiente. As fronteiras de `Pedidos` e `Pagamento` já estão desenhadas pensando nessa extração futura.
