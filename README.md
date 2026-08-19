# java-marketplace

Backend de marketplace multi-vendedor, escrito em Java/Spring, com escopo deliberadamente
estreito e profundidade concentrada no núcleo: **pedido, pagamento e estoque**. Não é uma loja
completa — é uma demonstração de modelagem de domínio, concorrência, consistência e operação sob
falha. O nicho (tecnologia e cultura geek) é só o pano de fundo; a arquitetura é o ponto.

> Critério de sucesso do projeto: um revisor técnico abre o repositório e, em poucos minutos, vê
> rigor no fluxo de pedido (máquina de estados, idempotência de pagamento, estoque correto sob
> concorrência) — não 30 endpoints CRUD genéricos. Ver [design/escopo](notes/escopo.md) para os
> cortes deliberados de escopo.

## Tour guiado

Para quem tem pouco tempo, nessa ordem:

1. **[architecture/overview.md](architecture/overview.md)** — diagramas C4 (Context, Container,
   Component) e como os 6 módulos se comunicam entre si.
2. **[adr/](adr/)** — decisões arquiteturais significativas, uma por arquivo, imutáveis. Comece
   por [ADR-0001](adr/0001-monolito-modular.md) (monólito modular em vez de microsserviços).
3. **[design/nucleo-pedido-pagamento-estoque.md](design/nucleo-pedido-pagamento-estoque.md)** —
   como o núcleo (pedido, pagamento, estoque) é desenhado antes de ser construído: máquina de
   estados, idempotência, outbox, consistência de estoque sob concorrência.
4. **[notes/](notes/)** — notas de referência menores, cada uma registrando o raciocínio por trás
   de uma decisão técnica pontual (ex.: por que a busca do catálogo virou uma query nativa).

## Arquitetura em uma frase

**Monólito modular.** Um único deploy, seis bounded contexts (`identity`, `catalog`, `estoque`,
`pedidos`, `pagamento`, `notificação`) com fronteiras impostas pelo Spring Modulith e verificadas
em teste — nenhum módulo acessa os internals de outro, só a API pública que ele decide expor.
DDD tático e hexagonal são aplicados seletivamente, só onde há invariante real para proteger
(`Pedidos`, `Pagamento` e o núcleo pequeno de `Estoque`); os módulos de apoio permanecem
simples/CRUD de propósito. Razão completa em [ADR-0001](adr/0001-monolito-modular.md).

## Stack

- **Java 21** · **Spring Boot** (Web MVC, Security, Data JPA, Validation)
- **PostgreSQL** com **Flyway** para migrations (inclui busca full-text via `tsvector`/GIN)
- **Spring Modulith** para fronteiras de módulo verificadas em build
- **JWT** (`java-jwt`) para autenticação, `BCrypt` para senha
- **Vavr** (`Either`) para erros de negócio como valor, em vez de exceção — ver
  [ADR-0004](adr/0004-erros-de-negocio-como-valor.md)
- **springdoc-openapi** + **Scalar** para documentação interativa da API
- **Testcontainers** (Postgres real em teste de integração)
- **Spotless** com formatador Eclipse JDT (tabs) — ver [ADR-0005](adr/0005-formatacao-tab-eclipse-jdt.md)

## Status atual

Em desenvolvimento incremental por fatia vertical. Hoje:

- **`identity`** — cadastro e login completos (endpoint HTTP → caso de uso → persistência),
  papéis `USER`/`SELLER`/`ADMIN`, emissão de JWT.
- **`catalog`** — domínio, persistência e casos de uso de vendedores/categorias/produtos, com
  busca pública combinável (categoria + faixa de preço + full-text). Ainda sem camada HTTP
  própria.
- **`estoque`, `pedidos`, `pagamento`, `notificação`** — ainda não implementados; o desenho está
  em [design/nucleo-pedido-pagamento-estoque.md](design/nucleo-pedido-pagamento-estoque.md),
  escrito antes da construção.

## Rodando localmente

Pré-requisitos: Java 21 (ver [.sdkmanrc](.sdkmanrc)) e um PostgreSQL acessível.

```bash
cp .env.example .env
# edite .env com as credenciais do seu Postgres local

./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`; documentação interativa em `/scalar`. Testes de
integração usam Testcontainers e sobem um Postgres descartável automaticamente:

```bash
./mvnw test
```
