# Arquitetura — visão geral (C4)

Este documento usa o [C4 Model](https://c4model.com/) para descrever a arquitetura do
`java-marketplace` em três níveis de zoom: **Context**, **Container** e **Component**. Não há
nível **Code** — como o [guia de referência desta skill](https://c4model.com/) recomenda, esse
nível compensa pouco desenhado à mão e a IDE já gera diagrama de classes sob demanda quando
necessário.

Os diagramas são feitos em [Mermaid](https://mermaid.js.org/) (`C4Context`/`C4Container`/
`C4Component`), que renderiza direto no GitHub. O suporte do Mermaid a C4 é experimental — a
notação de cor é livre; a legenda abaixo de cada diagrama é o que fixa o significado.

Para o *porquê* das fronteiras de módulo e das regras de comunicação usadas aqui, ver
[ADR-0001](../adr/0001-monolito-modular.md) e [notes/fluxo-cross-module-ownership.md](../notes/fluxo-cross-module-ownership.md).

---

## Nível 1 — System Context

Visão de fora: quem usa o sistema e com que outros sistemas ele conversa. `java-marketplace` é
uma caixa só.

```mermaid
C4Context
    title System Context — java-marketplace

    Person(cliente, "Cliente", "Navega no catálogo, faz pedido, paga")
    Person(vendedor, "Vendedor", "Cadastra produtos, gerencia estoque próprio, acompanha pedidos")
    Person(admin, "Admin", "Visão operacional mínima")

    System(marketplace, "java-marketplace", "Backend de marketplace multi-vendedor. Monólito modular: identidade, catálogo, estoque, pedidos, pagamento, notificação.")

    System_Ext(gateway, "Gateway de pagamento", "Cobrança e confirmação assíncrona via webhook. Mockado nesta implementação (stub local / WireMock) — ver notes/escopo.md.")

    Rel(cliente, marketplace, "navega, faz pedido, acompanha status", "HTTPS/JSON")
    Rel(vendedor, marketplace, "cadastra produtos, gerencia estoque, separa/despacha pedidos", "HTTPS/JSON")
    Rel(admin, marketplace, "opera com visão administrativa mínima", "HTTPS/JSON")
    Rel(marketplace, gateway, "inicia cobrança com chave de idempotência", "HTTPS")
    Rel(gateway, marketplace, "confirma ou recusa pagamento", "HTTPS webhook")

    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="1")
```

**Legenda:** setas cheias = chamada HTTP síncrona (pedido/resposta). `java-marketplace` é o único
sistema desenvolvido neste repositório; o gateway de pagamento é o único sistema externo — está
mockado hoje (ver [notes/escopo.md](../notes/escopo.md)), mas é modelado como externo porque, na
intenção arquitetural, é sempre um sistema de terceiros.

---

## Nível 2 — Container

Zoom para dentro de `java-marketplace`. Como o sistema é um **monólito modular** por decisão
explícita ([ADR-0001](../adr/0001-monolito-modular.md)), este nível é propositalmente enxuto: há
um único container executável. A riqueza dos seis bounded contexts só aparece no nível
**Component**, a seguir — a magreza deste diagrama é, ela mesma, a prova visual da decisão do
ADR-0001 (nenhum módulo virou processo/deploy próprio).

```mermaid
C4Container
    title Container — java-marketplace

    Person(cliente, "Cliente / Vendedor / Admin", "Atores autenticados via JWT")

    System_Boundary(sys, "java-marketplace") {
        Container(api, "API Application", "Spring Boot (Java 21)", "Monólito modular com fronteiras Spring Modulith. Expõe API REST autenticada por JWT; orquestra os 6 módulos de domínio.")
        ContainerDb(db, "Database", "PostgreSQL + Flyway", "Dados transacionais de todos os módulos + índice full-text (tsvector/GIN) do catálogo. Migrations versionadas.")
    }

    System_Ext(gateway, "Gateway de pagamento", "Mockado nesta implementação")

    Rel(cliente, api, "usa a API", "HTTPS/JSON + JWT")
    Rel(api, db, "lê/escreve", "JDBC")
    Rel(api, gateway, "inicia cobrança", "HTTPS")
    Rel(gateway, api, "confirma pagamento", "HTTPS webhook")

    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="1")
```

**Legenda:** um único deploy (`API Application`) fala com um único banco relacional. Não há
fila/broker: eventos de domínio entre módulos são in-process (Spring Modulith Event Publication
Registry — ver [design/nucleo-pedido-pagamento-estoque.md](../design/nucleo-pedido-pagamento-estoque.md)),
não um container à parte. Caminho de extração futura (um módulo virar processo próprio, broker
real) preservado, mas não construído — gatilho de revisão descrito no ADR-0001.

---

## Nível 3 — Component (zoom em `API Application`)

Este é o diagrama com mais sinal do conjunto: zoom para dentro do único container, mostrando os
seis bounded contexts e — o ponto central — **como cada um se comunica com os outros**: chamada
síncrona (quando a resposta decide o que fazer agora) ou evento de domínio assíncrono (quando
não). A regra e os exemplos vêm de [ADR-0001](../adr/0001-monolito-modular.md) e
[notes/fluxo-cross-module-ownership.md](../notes/fluxo-cross-module-ownership.md).

```mermaid
C4Component
    title Component — módulos dentro de API Application

    Container_Boundary(api, "API Application") {
        Component(identity, "Identity", "Spring Modulith module", "Usuários, papéis (USER/SELLER/ADMIN), credenciais, emissão de JWT. Módulo de apoio, upstream — não conhece nenhum outro módulo.")
        Component(catalog, "Catalog", "Spring Modulith module", "Vendedores, categorias, produtos, busca pública combinável. Módulo de apoio (CRUD).")
        Component(estoque, "Estoque", "Spring Modulith module", "Nível por produto; reserva/baixa/liberação. Núcleo rico pequeno: invariante 'estoque nunca negativo' protegida por locking otimista.")
        Component(pedidos, "Pedidos", "Spring Modulith module", "Agregado Pedido com máquina de estados própria. Orquestra o checkout: consulta catálogo, reserva estoque, aciona pagamento.")
        Component(pagamento, "Pagamento", "Spring Modulith module", "Cobrança idempotente via gateway (mock), confirmação por webhook, publica confirmação/recusa como evento.")
        Component(notificacao, "Notificação", "Spring Modulith module", "Reage a eventos de domínio (pedido pago, enviado) disparando e-mail mockado/log. Sem entidade própria.")
    }

    Rel(catalog, identity, "concede papel SELLER via porta publicada (grantRole)", "síncrono")
    Rel(pedidos, catalog, "consulta preço/disponibilidade ao criar pedido", "síncrono")
    Rel(pedidos, estoque, "reserva estoque (comando)", "síncrono")
    Rel(pedidos, pagamento, "inicia cobrança com chave de idempotência", "síncrono")
    Rel(pagamento, pedidos, "PaymentConfirmed / PaymentFailed", "evento assíncrono (outbox)")
    Rel(pedidos, estoque, "OrderPaid → baixa definitiva do estoque reservado", "evento assíncrono (outbox)")
    Rel(pedidos, notificacao, "OrderPaid / OrderShipped / OrderDelivered", "evento assíncrono (outbox)")

    UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="1")
```

**Legenda:**

- **Síncrono** (linha "síncrono" no rótulo) — usado só quando o módulo chamador precisa da
  resposta *agora* para decidir o que fazer. Cria dependência de compilação na direção da seta.
- **Evento assíncrono** (linha "evento assíncrono (outbox)") — o publicador não conhece os
  assinantes; não cria dependência de compilação nenhuma. É o que mantém o grafo entre módulos
  **acíclico** — verificado em teste pelo Spring Modulith. Note que `Pedidos ↔ Pagamento` usa os
  dois modos em direções diferentes (comando síncrono para *iniciar*, evento para *confirmar*)
  justamente para não formar um ciclo de dependência síncrona entre os dois.
- Seta de dependência sagrada: `catalog → identity`, nunca o inverso (identity não sabe o que é
  `Seller`) — ver [notes/fluxo-cross-module-ownership.md](../notes/fluxo-cross-module-ownership.md).
- Detalhe completo de cada evento e da idempotência de pagamento:
  [design/nucleo-pedido-pagamento-estoque.md](../design/nucleo-pedido-pagamento-estoque.md).

Não há diagrama de Component para os módulos de apoio (`identity`, `catalog`, `notificação`) —
são CRUD/anêmicos de propósito e não têm complexidade interna que justifique mais um zoom.
