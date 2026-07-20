# ADR-0004 — Erros de negócio como valor (`Either`) em vez de exceção

- **Status:** Aceito
- **Data:** 2026-07-20
- **Decisores:** Caio Marques Silva
- **Contexto:** como um caso de uso sinaliza falha esperada, transversal a todos os módulos (ver [ADR-0002](0002-seguranca-no-shared-emissao-no-identity.md) e [ADR-0003](0003-organizacao-de-pacotes-e-convencao-de-nomes.md))

## Contexto

A primeira fatia (`RegisterUser`) nasceu sinalizando falha de negócio com
`throw new ResponseStatusException(...)` dentro do caso de uso. Isso trouxe um problema de
camada: `ResponseStatusException` é um tipo **de HTTP** (`spring-web`), e o caso de uso é
**camada de aplicação** — que não deveria conhecer o transporte. O mesmo vazamento que já
havíamos eliminado nos DTOs (request/response de web separados de params/result de aplicação)
reaparecia no tratamento de erro.

Há duas naturezas de falha que o código precisa distinguir:

- **Esperada / de regra de negócio** — "e-mail já cadastrado", "ADMIN não pode se
  auto-registrar". Faz parte do contrato do caso de uso; o chamador *deve* lidar com ela. Não é
  excepcional — é um resultado possível.
- **Inesperada** — bug, banco fora do ar, invariante violada. Aí sim excepcional.

Modelar a primeira como exceção mistura as duas: some do tipo de retorno (o chamador não é
lembrado de tratá-la), e arrasta a decisão de status HTTP para dentro do domínio.

## Decisão

**Caso de uso sinaliza falha esperada como valor, com `io.vavr.control.Either<Erro, Result>` —
nunca `throw` para regra de negócio.**

- O lado direito (`Right`) é o resultado de sucesso (`...Result`); o esquerdo (`Left`) é o erro.
- **A tradução erro → transporte vive na borda.** O controller consome o `Either` (`fold`) e
  mapeia cada erro para o status HTTP; o caso de uso não conhece HTTP.
- **Erro modelado como `sealed interface` + `record` por caso de uso** (ex.:
  `RegisterUserError` com `AdminSelfRegistration` e `EmailAlreadyInUse(String email)`). O selado
  habilita `switch` **exaustivo verificado pelo compilador** — adicionar um erro novo quebra o
  build até que a borda o trate. O record carrega o dado do erro (o e-mail em conflito mora no
  próprio erro).
- **Exceção fica reservada para o inesperado** — bug, indisponibilidade de infra. Essas sobem e
  são tratadas pelo mecanismo padrão do Spring (handler global), não pelo `Either`.
- **Lib:** vavr **0.10.4 (estável)**. A série 1.0.0 nunca saiu de `alpha`; usar a estável evita
  API instável debaixo do projeto.

## Consequências

### Positivas

- O caso de uso volta a ser **puro de transporte**: testável sem subir Spring, reaproveitável
  por qualquer borda (HTTP hoje; mensageria/CLI amanhã) sem reescrever o tratamento de erro.
- O erro faz parte da **assinatura** — o chamador é obrigado a lidar com o `Either` (não há como
  "esquecer" de tratar, como acontece com exceção não checada).
- `switch` exaustivo sobre o sealed: o compilador é a rede de segurança quando surgir um erro
  novo.
- Convenção única e transversal — login, catálogo, pedidos seguem o mesmo shape.

### Negativas / tradeoffs

- Curva de leitura para quem não conhece vavr/`Either`; é um estilo funcional menos familiar em
  Java corporativo.
- Uma dependência a mais (vavr) no núcleo.
- Boilerplate de mapeamento na borda (`fold` + `switch`) que a exceção "esconderia" num handler
  global — aceito em troca de manter o domínio livre de HTTP e a falha no tipo.
- `Either` de vavr não acumula erros — para múltiplas violações de uma vez seria preciso outro
  tipo (ver alternativas).

## Alternativas consideradas

**Exceção (`ResponseStatusException`) no caso de uso — rejeitada.** É o ponto de partida que
originou este ADR: arrasta HTTP para a camada de aplicação e tira o erro da assinatura.

**Exceção de domínio própria (ex.: `EmailAlreadyInUseException`) + `@RestControllerAdvice` —
descartada por ora.** Resolve o vazamento de HTTP (a exceção passa a ser de domínio), mas mantém
o erro *fora* do tipo de retorno e usa fluxo de exceção para caso esperado. Reavaliável se o
mapeamento por `fold` virar repetitivo entre muitos módulos.

**`Try<Result>` (vavr) — rejeitada.** Continua girando em torno de exceção (encapsula um
`throw`), não expressa *quais* erros de negócio são possíveis.

**Exceções checadas — rejeitadas.** Poluem assinaturas, não compõem bem com lambdas/streams e
não carregam dado estruturado com a ergonomia de um record.

**`Validation<Seq<Erro>, Result>` (vavr) — adiada.** É o tipo certo para **acumular** múltiplos
erros (ex.: validar vários campos e devolver todos). Hoje o primeiro erro que barra já basta;
adotável pontualmente no futuro onde acumular fizer sentido.

## Gatilho de revisão

Reconsiderar quando: (a) o encadeamento de `Either` virar cerimônia sem ganho de clareza; (b)
o mapeamento erro→status se repetir igual em muitos módulos, a ponto de um `@RestControllerAdvice`
com exceções de domínio compensar; ou (c) surgir necessidade real de **acumular** erros num caso
de uso — aí aquele caso migra para `Validation`.
