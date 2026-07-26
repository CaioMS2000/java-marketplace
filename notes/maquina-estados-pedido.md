# Máquina de estados — Pedido

> Documento vivo. v1. Especifica o comportamento do agregado `Pedido`: estados, transições legais, transições proibidas e os efeitos colaterais de cada transição (estoque, pagamento, eventos).
>
> A lógica de transição vive **dentro do agregado** (`pedido.confirmarPagamento()`, `pedido.cancelar(motivo)`, etc.). Cada método valida o estado atual e lança se a transição for ilegal. O application service orquestra os efeitos colaterais; a *regra* mora na entidade.

## Estados

| Estado | Significado | Estoque | Terminal? |
|---|---|---|---|
| `CRIADO` | Pedido criado e validado, aguardando pagamento. | **Reservado** | não |
| `PAGO` | Pagamento confirmado pelo gateway. | **Baixado** (definitivo) | não |
| `SEPARADO` | Vendedor separou/embalou os itens. | baixado | não |
| `ENVIADO` | Despachado para transporte. | baixado | não |
| `ENTREGUE` | Entregue ao cliente. | baixado | quase¹ |
| `CANCELADO` | Encerrado antes da entrega. | **liberado / devolvido** | sim |
| `DEVOLVIDO` | Devolvido após a entrega. | **devolvido** | sim |

¹ `ENTREGUE` só transita para `DEVOLVIDO`; fora isso é terminal.

## Transições legais

| De | Para | Gatilho (ator / evento) | Guarda | Efeitos colaterais |
|---|---|---|---|---|
| `CRIADO` | `PAGO` | webhook de pagamento confirmado | estado é `CRIADO`; pagamento válido e ainda não processado (idempotência) | reserva → baixa definitiva; publica `OrderPaidEvent` |
| `CRIADO` | `CANCELADO` | cliente cancela · **expiração** (scheduler) · pagamento recusado em definitivo | estado é `CRIADO` | libera a reserva; se houve cobrança, marca estorno |
| `PAGO` | `SEPARADO` | vendedor confirma separação | estado é `PAGO`; ator é o `SELLER` dos itens | — |
| `PAGO` | `CANCELADO` | cliente cancela · vendedor/admin cancela | estado é `PAGO` | devolve o estoque; marca estorno (`RefundRequestedEvent`) |
| `SEPARADO` | `ENVIADO` | vendedor despacha | estado é `SEPARADO`; ator é `SELLER` | publica `OrderShippedEvent` |
| `SEPARADO` | `CANCELADO` | **vendedor/admin** (não o cliente) | estado é `SEPARADO`; ator ∈ {`SELLER`, `ADMIN`} | devolve o estoque; marca estorno |
| `ENVIADO` | `ENTREGUE` | confirmação de entrega | estado é `ENVIADO` | publica `OrderDeliveredEvent` |
| `ENTREGUE` | `DEVOLVIDO` | cliente solicita devolução | estado é `ENTREGUE`; dentro da janela de devolução | devolve o estoque; marca estorno |

## Transições proibidas (e por quê)

- `ENVIADO` → `CANCELADO`: já está em transporte. O caminho depois do envio é **devolução pós-entrega**, não cancelamento.
- `CRIADO` → `SEPARADO` / `ENVIADO` / `ENTREGUE`: não se pula o pagamento.
- `PAGO` → `ENVIADO`: não se pula a separação.
- Qualquer retrocesso (`PAGO` → `CRIADO`, `ENVIADO` → `SEPARADO`, …): o fluxo não anda para trás.
- `CANCELADO` / `DEVOLVIDO` → qualquer: estados terminais, imutáveis.
- `ENTREGUE` → qualquer ≠ `DEVOLVIDO`: terminal exceto pela devolução.

Cada um desses deve ter um **teste** que prova que a chamada lança exceção. É isso que prova que a máquina de estados é real, e não só um diagrama.

## Decisões de design não-óbvias

**1. Criação é atômica.** Se qualquer item falha na validação (inexistente, desativado, sem estoque, vendedor suspenso), o pedido inteiro é rejeitado e nada é reservado. Sem pedido parcial — isso evita pagamento parcial e divisão de pedido, que estão fora de escopo.

**2. Reserva expira.** `CRIADO` não pode segurar estoque reservado para sempre. Um scheduler cancela pedidos `CRIADO` mais velhos que a janela de pagamento (sugestão: 30 min), liberando a reserva. Sem isso, estoque "vaza" — fica preso em pedidos que nunca serão pagos. Essa é a regra que justifica a transição `CRIADO` → `CANCELADO` por expiração.

**3. Transições são idempotentes pela guarda de estado de origem.** O webhook de pagamento pode chegar duas vezes (retry da rede). Como `confirmarPagamento()` só age se o estado é `CRIADO`, a segunda chamada num pedido já `PAGO` é um no-op — não baixa estoque duas vezes, não publica o evento duas vezes. A idempotência cai da própria máquina de estados.

**4. Janela de cancelamento depende do ator.**
- **Cliente**: pode cancelar em `CRIADO` (trivial, só libera reserva) e em `PAGO` antes da separação (precisa de estorno). Depois de `SEPARADO`, não — usa devolução pós-entrega.
- **Vendedor / Admin**: poderes mais amplos (cancelar até `SEPARADO`), para casos como estoque divergente ou fraude.

**5. Corrida: cancelamento vs. webhook de pagamento.** O cliente cancela um `CRIADO` no mesmo instante em que o webhook de "pagamento confirmado" chega. Resolução: o cancelamento leva o pedido a `CANCELADO`; quando o webhook chega para um pedido já `CANCELADO`, a guarda barra a transição para `PAGO`, e o sistema reconhece que houve uma cobrança sem pedido vivo → dispara estorno. Cobrou, mas o pedido morreu: devolve o dinheiro. Esse caminho é justamente o tipo de cenário que vale um teste dedicado.

**6. Devolução sem estorno real (MVP).** `DEVOLVIDO` devolve o estoque e publica `RefundRequestedEvent`, mas o fluxo financeiro de estorno de verdade está fora de escopo. O evento existe (a costura está pronta); o consumidor que efetua o estorno é trabalho futuro.

## Eventos publicados

Todos via outbox, para garantir consistência entre a mudança de estado e a publicação:
`OrderPaidEvent`, `OrderShippedEvent`, `OrderDeliveredEvent`, `OrderCancelledEvent`, `RefundRequestedEvent`.

No monólito modular, são eventos in-process (Spring Modulith). Na fase 4, o publisher passa a ser Kafka sem o domínio perceber.
