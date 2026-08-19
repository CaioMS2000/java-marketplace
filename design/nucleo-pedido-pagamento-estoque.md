# Design: núcleo — Pedido, Pagamento e Estoque

> Documento vivo. v1. Escrito **antes** da implementação de `estoque`, `pedidos` e `pagamento` —
> `identity` e `catalog` (módulos de apoio) já estão prontos. Ver estado atual em
> [README.md](../README.md#status-atual).

## Problema

O núcleo do marketplace é o fluxo de checkout: cliente monta um pedido a partir de produtos do
catálogo, o sistema reserva estoque, cobra via gateway, e evolui o pedido por uma máquina de
estados até a entrega — tudo isso com dinheiro e concorrência real envolvidos. É deliberadamente
a parte mais difícil do projeto (ver [notes/escopo.md](../notes/escopo.md)): três invariantes
têm que se sustentar ao mesmo tempo —

1. **Estoque nunca fica negativo**, mesmo com duas reservas concorrentes disputando a última
   unidade.
2. **Uma cobrança por tentativa lógica de pagamento**, mesmo que o cliente ou o gateway repita a
   requisição/webhook.
3. **O estado do pedido e o evento publicado nunca divergem** — se o pedido virou `PAGO`, o
   evento `OrderPaid` é publicado; se a publicação falhar, ela é retentada, nunca perdida.

Identity e Catalog já provaram a tubulação (auth, persistência, módulos Spring Modulith). Este
documento fixa o desenho de Estoque, Pedidos e Pagamento **antes** de escrever a primeira linha
deles, para que as decisões não-óbvias (idempotência, outbox, consistência sob concorrência)
sejam explícitas em vez de descobertas no meio da implementação.

## Requisitos e não-requisitos

**Dentro do escopo do núcleo** (detalhe completo em [notes/escopo.md](../notes/escopo.md)):

- Estoque: nível por produto; reserva no fechamento do pedido; baixa definitiva na confirmação
  do pagamento; liberação no cancelamento; correta sob concorrência.
- Pedidos: criação atômica a partir de itens selecionados; máquina de estados com transições
  legais e proibidas explícitas (spec completa em
  [notes/maquina-estados-pedido.md](../notes/maquina-estados-pedido.md)); lógica de transição no
  agregado, não no service.
- Pagamento: cobrança via gateway mock; idempotência por tentativa; confirmação assíncrona via
  webhook; consistência entre mudança de estado e evento publicado (outbox).

**Explicitamente fora de escopo** (cortes que liberam profundidade no que importa — lista
completa em [notes/escopo.md](../notes/escopo.md)):

- Carrinho persistente, split de pagamento entre vendedores, frete real (cotação/transportadora),
  tokenização de cartão/conformidade PCI, multi-moeda, devolução com estorno financeiro completo
  (o estado `DEVOLVIDO` existe; o reembolso real não), rate limiting e observabilidade completa.
- Pedido parcial: se qualquer item falhar validação, o pedido inteiro é rejeitado — sem dividir
  em "o que deu certo" e "o que não deu".

## Abordagem proposta

### Máquina de estados do Pedido

Especificação completa (estados, transições legais/proibidas, guardas, efeitos colaterais) já
está em [notes/maquina-estados-pedido.md](../notes/maquina-estados-pedido.md) — não duplicada
aqui para não divergir. Resumo do que importa para este documento: cada transição só ocorre se o
agregado estiver no **estado de origem esperado** (a guarda de estado). Essa guarda é o mecanismo
que dá idempotência de graça às transições — ponto retomado abaixo, na confirmação de pagamento.

### Criação do pedido (checkout)

1. `Pedidos` consulta `Catalog` **de forma síncrona** para preço e disponibilidade declarada dos
   itens (precisa da resposta agora para decidir se o pedido é viável) e grava um **snapshot** de
   preço no pedido — o preço não muda depois se o produto for reprecificado.
2. Validação é **tudo ou nada**: qualquer item inválido (inexistente, desativado, sem estoque,
   vendedor suspenso) rejeita o pedido inteiro; nada é reservado.
3. `Pedidos` chama `Estoque` **de forma síncrona** com um comando `reservar(produtoId, qtd)` por
   item — é um comando, não uma consulta: falha se não houver saldo. O pedido só é criado
   (`CRIADO`) se todas as reservas tiverem sucesso.
4. `Pedidos` chama `Pagamento` **de forma síncrona** para iniciar a cobrança, passando a chave de
   idempotência (ver abaixo). A resposta síncrona aqui é só "cobrança aceita para processamento",
   não a confirmação — essa vem depois, assíncrona.

### Consistência de estoque sob concorrência

`StockItem` carrega nível total e reservado; disponível = total − reservado. A invariante
"reservado nunca excede total" é protegida por **locking otimista** (`@Version` do JPA): a leitura
do saldo e a escrita da reserva acontecem na mesma transação curta, e uma segunda transação
concorrente que leu o mesmo saldo desatualizado falha ao commitar (`OptimisticLockException`) em
vez de reservar sobre um saldo que já não existe.

**Decisão: no conflito de lock, falhar e devolver erro de negócio ao cliente** ("estoque
indisponível, tente novamente"), sem retry automático no servidor. Ver alternativas consideradas
e trade-off abaixo.

### Idempotência de pagamento

**Decisão: chave de idempotência gerada pelo cliente**, um UUID novo por tentativa de pagamento,
enviado no request de checkout. `Pagamento` trata essa chave como única (constraint no banco): a
mesma chave nunca gera duas cobranças, mesmo que o request chegue duplicado (retry de rede do
cliente). Uma nova tentativa **intencional** depois de uma falha usa uma chave nova — é assim que
o padrão se diferencia de "derivar a chave do pedido", que colidiria tentativa nova com tentativa
antiga.

### Confirmação assíncrona e o evento entre Pagamento e Pedidos

O gateway confirma (ou recusa) via **webhook HTTP**, que pode chegar mais de uma vez (retry do
lado do gateway). `Pagamento` registra a confirmação de forma idempotente (mesma chave →
no-op na segunda chegada) e então **publica um evento** (`PaymentConfirmed` / `PaymentFailed`) —
não faz uma chamada síncrona de volta para `Pedidos`.

Isso é deliberado, não só "porque webhook é assíncrono": `Pedidos → Pagamento` já é uma chamada
síncrona (passo 4 acima, para iniciar a cobrança). Se `Pagamento → Pedidos` também fosse
síncrona para confirmar, os dois módulos formariam um **ciclo de dependência de compilação** —
exatamente o que o Spring Modulith verifica e barra em teste (ver
[ADR-0001](../adr/0001-monolito-modular.md)). Usar evento numa direção quebra o ciclo: `Pedidos`
escuta `PaymentConfirmed`/`PaymentFailed` e chama `pedido.confirmarPagamento()` /
`pedido.cancelar(...)` — e como esses métodos só agem se o pedido estiver no estado de origem
esperado (`CRIADO`), uma entrega duplicada do evento é um no-op. A idempotência da transição cai
de graça da própria máquina de estados (já registrado em
[notes/maquina-estados-pedido.md](../notes/maquina-estados-pedido.md), decisão 3).

O mesmo padrão vale para o resto da cadeia: quando `Pedidos` entra em `PAGO`, publica
`OrderPaid`; `Estoque` (baixa definitiva) e `Notificação` (e-mail) escutam independentemente,
sem que `Pedidos` saiba que existem.

### Outbox

**Decisão: usar o Event Publication Registry do Spring Modulith**, em vez de uma tabela outbox
própria com poller manual. O projeto já adotou Spring Modulith para fronteira de módulo
([ADR-0001](../adr/0001-monolito-modular.md)); o registry dá o mesmo resultado de um outbox
clássico — o evento é persistido na **mesma transação** da mudança de estado, e uma publicação
que falhou (listener lançou exceção, processo caiu no meio) é **retentada** automaticamente —
sem reimplementar esse mecanismo à mão.

## Alternativas consideradas

**Idempotência: chave derivada do `orderId`** — descartada. Mais simples (nenhum campo extra no
request), mas colide "uma cobrança por pedido" com "uma cobrança por tentativa": não dá pra
distinguir uma tentativa nova, intencional, depois de uma falha anterior. UUID por tentativa,
gerado pelo cliente, é o padrão usado por gateways reais (Stripe et al.) e resolve isso.

**Outbox: tabela própria + poller** — descartada por ora. Implementar o padrão do zero
demonstraria entender o mecanismo, mas duplicaria o que o Event Publication Registry já dá,
sem ganho de sinal adicional — o projeto já usa Spring Modulith para a fronteira de módulo, então
usar o registry é consistente com essa escolha, não uma dependência nova.

**Conflito de estoque: retry automático no servidor** — descartada por ora. Reduziria a taxa de
erro percebida pelo cliente sob contenção momentânea, mas mascara a contenção real e adiciona
complexidade (quantas tentativas? qual backoff?) sem necessidade clara no volume esperado de um
projeto de portfólio. Falhar e devolver erro é mais simples e mais honesto sobre o que aconteceu;
fica como candidato de revisão se a decisão precisar mudar sob carga real.

**Confirmação de pagamento como chamada síncrona `Pagamento → Pedidos`** — descartada. Criaria um
ciclo de dependência entre os dois módulos (ver seção acima), o que o Spring Modulith rejeitaria
em teste. Evento assíncrono mantém o grafo de dependência entre módulos acíclico.

## Riscos e trade-offs

- **Chave de idempotência do lado do cliente** exige que quem consome a API gere um UUID
  corretamente a cada tentativa nova; reusar por engano bloqueia uma tentativa legítima. Mitigar
  documentando o contrato no OpenAPI (`Idempotency-Key` como campo obrigatório e seu significado).
- **Fail-fast no conflito de estoque** pode devolver erro ao cliente sob picos de concorrência
  alta sobre o mesmo produto, mesmo quando uma segunda tentativa teria sucesso. Aceitável para o
  volume de um projeto de portfólio; revisitar (retry limitado, ou fila de reserva) se o projeto
  algum dia enfrentar carga real.
- **Listener de evento não-idempotente é o elo fraco do outbox.** O Event Publication Registry
  garante que o evento *será* entregue (retry automático), não que o listener trata reentrega
  corretamente. `Pedidos` já ganha isso de graça pela guarda de estado; os listeners de `Estoque`
  (baixa definitiva) e `Notificação` (e-mail) precisam da mesma disciplina — verificar se a baixa
  já ocorreu antes de aplicar, e aceitar reenvio de e-mail como não-crítico.
- **Reserva expira via scheduler.** Se o scheduler ficar fora do ar, reservas "vazam" (ficam
  presas em pedidos `CRIADO` nunca pagos) até ele voltar. Não há redundância desenhada para isso
  agora — aceitável para o escopo atual, registrado aqui para não ser esquecido.

## Plano

Seguindo o padrão de fatia vertical já usado em `identity`/`catalog` (walking skeleton → fatia
real, domínio a fundo, repete):

1. **Estoque** — `StockItem` (`@Version`), `reservar`/`baixar`/`liberar`, testes de concorrência
   provando a invariante sob duas transações simultâneas.
2. **Pedidos** — agregado `Pedido` com a máquina de estados completa (um teste por transição
   proibida, conforme [notes/maquina-estados-pedido.md](../notes/maquina-estados-pedido.md));
   caso de uso de criação chamando `Catalog` e `Estoque` de forma síncrona.
3. **Pagamento** — cobrança idempotente (chave única no banco) + endpoint de webhook mock +
   publicação de `PaymentConfirmed`/`PaymentFailed`.
4. **Fechar o loop** — `Pedidos` consome `PaymentConfirmed`/`PaymentFailed`; ao entrar em `PAGO`,
   publica `OrderPaid`; `Estoque` e `Notificação` consomem independentemente.
5. **Scheduler de expiração** — cancela `CRIADO` mais velho que a janela de pagamento, liberando
   a reserva.
6. **Notificação** — consumidor fino dos eventos de pedido, e-mail mockado/log.

Cada fase ganha teste de integração antes de avançar para a próxima — não há fase "volta depois
para testar".
