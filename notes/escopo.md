# Escopo — Marketplace (projeto de portfólio)

> Documento vivo. v1. O objetivo deste arquivo é **fixar fronteiras**, não detalhar implementação.

## Objetivo

Backend de marketplace multi-vendedor com escopo deliberadamente estreito e profundidade concentrada no núcleo: **pedido, pagamento e estoque**. Não é uma loja completa — é uma demonstração de modelagem de domínio, concorrência, consistência e operação sob falha.

**Critério de sucesso:** um revisor técnico abre o repositório e, em poucos minutos, vê rigor no fluxo de pedido (máquina de estados, idempotência de pagamento, estoque correto sob concorrência) — não 30 endpoints CRUD genéricos.

## Macro nicho sugerido

Marketplace de **tecnologia e cultura geek**, abrangendo categorias como computadores, smartphones, componentes, periféricos, games, livros, quadrinhos, jogos de tabuleiro e colecionáveis. O nicho orienta os dados e exemplos do projeto sem restringir sua arquitetura.

Esse macro nicho possibilita explorar:

- Produtos físicos e digitais.
- Produtos com variações, como cor, tamanho e plataforma.
- Marcas e fabricantes.
- Pré-venda e produtos usados.
- Estoque, garantia e diferentes métodos de entrega.
- Vendedores distintos oferecendo o mesmo produto.

## Atores

- **Cliente (USER)** — navega no catálogo, faz pedido, paga.
- **Vendedor (SELLER)** — cadastra produtos, gerencia o estoque dos próprios itens, acompanha pedidos.
- **Admin (ADMIN)** — visão operacional mínima.

## Dentro do escopo (IN)

O mínimo necessário para o fluxo nuclear funcionar ponta a ponta.

**Identidade**
- Cadastro e login de Cliente e Vendedor.
- Papéis `USER` / `SELLER` / `ADMIN`, autorização por papel.
- JWT.

**Catálogo** — *CRUD simples, modelo anêmico apropriado*
- Vendedor cria, edita e lista os próprios produtos.
- Cliente lista, busca (filtro simples por nome/categoria) e vê o detalhe.

**Estoque** — *núcleo rico pequeno*
- Nível por produto.
- Reserva no fechamento do pedido; baixa definitiva na confirmação do pagamento; liberação no cancelamento.
- Correção garantida sob concorrência (a invariante "estoque nunca negativo" vive na entidade).

**Pedidos** — *núcleo*
- Criação a partir de itens selecionados.
- Máquina de estados com regras de transição (especificada em documento próprio).
- Pedido como agregado; lógica de transição dentro da entidade, não no service.

**Pagamento** — *núcleo*
- Cobrança via gateway mock (stub local / WireMock).
- Idempotência (chave de idempotência — uma cobrança por tentativa lógica).
- Confirmação assíncrona via webhook.
- Outbox para consistência entre a mudança de estado e o evento publicado.

**Notificação** — *quase sem entidade*
- Reage a eventos de domínio (pedido pago, pedido enviado) com envio de e-mail mockado / log.

## Fora do escopo (OUT)

Cortado de propósito. **Os cortes são a decisão de design mais importante deste documento** — são eles que liberam profundidade no núcleo.

- Carrinho persistente / multi-dispositivo — o pedido se forma direto de itens; carrinho, se existir, é trivial.
- Recomendação, ranking, busca avançada / full-text — busca é filtro simples.
- Avaliações, notas, reviews.
- Chat ou mensageria entre comprador e vendedor.
- Cupons, promoções, descontos, fidelidade.
- Multi-moeda, internacionalização, cálculo de impostos.
- Split de pagamento entre vendedores, repasse e *payout*.
- Frete real: cotação, cálculo, integração com transportadora — status de envio é transição manual/mockada.
- Pagamento real: tokenização de cartão, conformidade PCI — o gateway é sempre mock.
- Múltiplos endereços, lista de desejos, histórico de navegação.
- Painel administrativo rico, relatórios, BI.
- Devolução com fluxo financeiro de estorno completo — o estado `DEVOLVIDO` existe; o reembolso real, não.
- Microsserviços, multi-tenant, multi-região.

**Candidatos a extensão futura** (só se a intenção for aprofundar *mais um eixo*, nunca largura): mensageria via Kafka substituindo os eventos in-process; estorno real no fluxo de devolução; rate limiting e observabilidade completa. Tudo isso é fase posterior — não MVP.

## O que este documento NÃO é

Não é uma lista exaustiva de requisitos funcionais. RFs detalhados serão escritos **apenas para o núcleo** (pedido / pagamento / estoque), onde o comportamento não é óbvio. O resto é CRUD e se documenta sozinho no código.
