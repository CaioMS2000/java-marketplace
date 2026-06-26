# ADR-0002 — Infraestrutura de segurança no `shared`, emissão de token no `identity`

- **Status:** Aceito
- **Data:** 2026-06-26
- **Decisores:** Caio Marques Silva
- **Contexto:** organização dos pacotes de segurança/JWT no monólito modular (ver [ADR-0001](0001-monolito-modular.md))

## Contexto

Ao montar a primeira fatia do módulo `identity` (registro de usuário), surgiu a pergunta de onde colocar as classes relacionadas a autenticação JWT: dentro do módulo `identity` (já que autenticar parece responsabilidade dele) ou numa área compartilhada.

O termo "autenticação" esconde **duas responsabilidades distintas**:

- **Emitir identidade** — verificar credenciais e *gerar* um token afirmando "este é fulano, com estes papéis". É específico do domínio de identidade (login, checagem de senha, decisão de quais claims o token carrega).
- **Impor segurança em toda requisição** — o filtro que roda em *todo* request, valida a assinatura do token e popula o `SecurityContext`; a configuração da filter chain do Spring Security. Isso é transversal: gateia `Catálogo`, `Pedidos`, `Pagamento` — todos os módulos.

A força decisiva é a **regra de direção de dependência** do [ADR-0001](0001-monolito-modular.md): nada transversal pode depender de um módulo de domínio; módulos de domínio podem depender do transversal. O `SecurityFilter` valida o token e extrai subject + papéis puramente das *claims* — nunca consulta a tabela `users` nem chama o `identity`. Logo, ele já é independente do domínio por construção. Colocá-lo dentro de `identity` faria todo módulo depender de `identity` só para ter segurança nas requisições — uma dependência indevida de todos para um.

## Decisão

Separar as duas responsabilidades por pacote:

- **`shared/security`** — infraestrutura transversal de segurança: `SecurityConfig` (filter chain, regras de acesso), `SecurityFilter` (validação por requisição), a primitiva de token `JWTProvider` (assinar **e** validar), `JwtConfig` (bean `Algorithm`/chave de assinatura) e o `PasswordEncoder`.
- **`shared/openapi`** — documentação da API (`SwaggerConfig`, `SwaggerUiDarkThemeTransformer`), também transversal.
- **`identity`** — registro, login (futuro), entidade `User`, e a *decisão* do conteúdo do token (subject, papéis). O login chamará a primitiva `JWTProvider.generateJwtToken(...)` do `shared` para assinar.

Critério prático para classificar qualquer peça futura: **"`Catálogo`/`Pedidos` precisariam disto mesmo se o módulo `identity` não existisse?"** Se sim, é `shared`; se não, é do módulo.

Resultado da dependência: `identity → shared`, em direção única, sem ciclo. O `identity` decide *o quê* vai no token; o `shared` provê *como* assinar/validar.

## Consequências

### Positivas

- O `SecurityFilter` permanece livre de dependência de domínio — valida tokens para todos os módulos sem conhecer nenhum.
- Direção de dependência única (`identity → shared`), coerente com a verificação de fronteiras do Spring Modulith (ADR-0001).
- Cada novo módulo de domínio herda a segurança do `shared` sem reimplementar nada nem criar acoplamento cruzado.

### Negativas / tradeoffs

- A "autenticação" fica fisicamente repartida (emissão no `identity`, validação no `shared`), o que pode surpreender quem espera tudo num módulo `auth` único. Mitigado por este ADR.
- `JWTProvider` concentra assinar **e** validar no `shared`; o lado de assinatura é usado só pelo `identity`. Aceito por simplicidade — ambos compartilham a mesma `Algorithm`, e separá-los agora seria cerimônia sem ganho.

## Alternativas consideradas

**Tudo de auth dentro de `identity` — rejeitado.** Tornaria todo módulo dependente de `identity` para ter segurança nas requisições, violando a direção de dependência do ADR-0001.

**Um módulo `auth` dedicado (irmão dos de domínio) — rejeitado por ora.** Defensável, mas para o escopo atual o filtro/config são infra pura sem regra de negócio própria; um módulo só para isso adiciona estrutura sem profundidade. Reavaliável se a autenticação ganhar domínio próprio (ex.: OAuth, refresh tokens, sessões).

**Segurança no `shared`, emissão no `identity` — escolhido.** Respeita a direção de dependência, mantém o filtro transversal independente do domínio e coloca a decisão de conteúdo do token onde ela é uma regra de negócio.

## Gatilho de revisão

Reconsiderar a criação de um módulo `auth` dedicado quando a autenticação deixar de ser encanamento e passar a ter domínio próprio — por exemplo: refresh tokens com ciclo de vida e revogação, integração OAuth/OIDC, gestão de sessões, ou políticas de autorização complexas que não caibam como infra simples no `shared`.
