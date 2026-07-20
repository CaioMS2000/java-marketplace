# ADR-0003 — Organização de pacotes: transversal na raiz e convenção de nomes

- **Status:** Aceito
- **Data:** 2026-07-20
- **Decisores:** Caio Marques Silva
- **Contexto:** estrutura de pacotes do código sob `com.caioms.java_marketplace` (ver [ADR-0001](0001-monolito-modular.md) e [ADR-0002](0002-seguranca-no-shared-emissao-no-identity.md))

## Contexto

Ao materializar a primeira fatia em código, duas decisões estruturais precisaram ser fixadas
para não virarem inconsistência acumulada:

1. **Como marcar o que é transversal.** O [ADR-0002](0002-seguranca-no-shared-emissao-no-identity.md)
   nomeou a infra transversal como `shared/security` e `shared/openapi`. Na prática, um pacote
   `shared` acrescenta uma palavra que não carrega informação: o que ele quer dizer é "não
   pertence a nenhum módulo, logo qualquer módulo pode usar". Essa mesma informação já está na
   ausência de `modules/` no caminho.
2. **Convenção de nomes de segmento de pacote.** A convenção oficial do Java é segmento todo
   minúsculo e sem separador (`usecases`). Em nomes de mais de uma palavra isso prejudica a
   leitura, e a decisão precisa valer desde o começo para não ter de reescrever pacotes depois.

## Decisão

**1. Fronteira por posição, não por nome `shared`.** A infra transversal vive em pacotes na
**raiz** de `com.caioms.java_marketplace` (hoje `http.security` e `http.openapi`); os bounded
contexts vivem sob `modules.<contexto>`. A regra que substitui o `shared` do ADR-0002:

> **Estar fora de `modules/` É a marca de transversal** — qualquer módulo pode depender dele.
> Nada em `modules/<x>` pode ser importado por `modules/<y>` a não ser via API pública do módulo.

Isto **supersede apenas o naming** do ADR-0002 (`shared/…` → pacotes na raiz). A decisão de
fundo do 0002 — segurança/emissão de token repartidas por responsabilidade, com direção de
dependência única `modules.identity → (transversal)` — permanece integralmente válida.

**2. Convenção de nomes.**

- **Segmento de pacote com mais de uma palavra:** `snake_case` (`use_cases`, não `useCases`
  nem `usecases`). Casa com o `java_marketplace` já presente na base e separa melhor num
  caminho pontilhado.
- **Segmento sempre inicia com minúscula** (`use_cases`, nunca `Use_cases`), para não haver
  ambiguidade "pacote ou classe?" num import.
- **Classe, interface, método, campo e constante seguem o padrão Java** (PascalCase /
  camelCase / UPPER_SNAKE). O desvio para `snake_case` vale **exclusivamente para segmento de
  pacote** — em nenhum outro identificador.

## Consequências

### Positivas

- A transversalidade fica legível pela posição no caminho, sem uma palavra dedicada; menos
  ruído, mesma informação.
- Convenção de nome única e decidida cedo — sem retrabalho de renomear pacote depois.
- Não há atrito de build: o `google-java-format`/Spotless formata espaçamento e ordem de
  import, mas nunca renomeia identificador ou segmento de pacote, então não briga com o
  `snake_case` nos pacotes.

### Negativas / tradeoffs

- `snake_case` em pacote **desvia da convenção oficial do Java** — um revisor pode estranhar.
  Aceito conscientemente em troca de legibilidade; documentado aqui para que seja lido como
  escolha, não descuido.
- Se um dia entrar **Checkstyle** com a regra `PackageName` default (`^[a-z]+(\.[a-z][a-z0-9]*)*$`),
  ela reprovaria os pacotes `snake_case`. Mitigação: ajustar o regex da regra, não os pacotes.
- Marcar transversal por posição (e não por um nome como `shared`) depende de disciplina/leitura
  do caminho; a verificação de fronteiras do Spring Modulith (ADR-0001) é quem torna isso
  executável, não apenas convencional.

## Alternativas consideradas

**Manter `shared/` (ADR-0002) — descartado.** Palavra redundante com a informação que a
ausência de `modules/` já dá; e "http" na raiz comunica melhor a natureza (encanamento web
transversal) do que "shared".

**Segmento de pacote em `camelCase` (`useCases`) — descartado.** No meio de um caminho
pontilhado, um segmento camelCase pode ser lido como nome de classe; `snake_case` separa de
forma mais clara e alinha com a base `java_marketplace`.

**Seguir a convenção oficial `usecases` (tudo minúsculo, junto) — descartado.** Nomes de mais
de uma palavra ficam difíceis de ler colados; a legibilidade foi priorizada sobre a
conformidade estrita.

## Gatilho de revisão

Reconsiderar quando: (a) entrar uma ferramenta de lint de nomes (ex.: Checkstyle) cujo custo de
configurar a exceção supere o ganho de legibilidade; ou (b) o projeto passar a ter mais de um
autor e a convenção não-padrão virar fonte recorrente de fricção em revisão.
