# Autenticação — níveis de ownership do fluxo

Nota de referência (não é ADR): as três abordagens para access/refresh token no ecossistema
Java/Spring, ordenadas por **quanto nós somos donos do fluxo de autenticação** — de "faço tudo"
a "delego tudo". Serve para lembrar as opções quando o refresh token / social login entrarem em
pauta (ver gatilho de revisão no [ADR-0002](../adr/0002-seguranca-no-shared-emissao-no-identity.md)).

## O eixo

```
A (dono de tudo) ───────── B (meio) ───────── C (IdP é dono de tudo)
```

## A) Emitir tudo (roll your own)

- **Nós somos donos do ciclo inteiro.** O Spring dá só as *primitivas* (encoder de JWT,
  `PasswordEncoder`, filter chain); a lógica é nossa: repositório de refresh, TTLs, **rotação**,
  **detecção de reuso**, **revogação**.
- **Custo:** mais trabalho e o mais fácil de errar sutilmente (segurança do refresh).
- **Quando compensa:** quando identity é um domínio que a gente *quer* construir (sinal de
  profundidade), e quando não se quer adotar o protocolo OAuth2 inteiro.
- **É onde estamos.** A costura conta/credencial (`User` = conta, `Credential` separado) mantém
  social login e refresh como adições futuras, sem rewrite.

## B) Spring Authorization Server

- Projeto **oficial** que transforma a app num **Authorization Server OAuth2/OIDC**. Implementa o
  token endpoint, o **grant de refresh, rotação, revogação e persistência** por nós.
- **Custo:** adota o **modelo OAuth2** (client_id, grant_type, consent…); mais pesado e reformata
  os endpoints. Pode ser over-engineering para login first-party simples.
- **Quando compensa:** quando se quer o padrão OAuth2 de verdade (ex.: emitir tokens para vários
  clients) sem escrever o protocolo na mão.

## C) IdP externo (Keycloak, Auth0, Cognito, Okta)

- O IdP é dono do **ciclo inteiro** (emitir, refresh, revogar, rotação, social login, MFA,
  sessões). Nossa app vira só **resource server** validando o access token — **nunca toca no
  refresh**. Social login vira configuração de admin (incl. account linking).
- **Custo:** deixamos de ser **donos da identidade** — a tabela de contas/login migra para o IdP,
  e o módulo `identity` encolhe para "resource server + perfil local". Contradiz "identity como
  coluna que construímos".
- **Quando compensa:** quando auth não é o ponto do projeto e se quer o menor esforço/maior
  robustez; Keycloak é o padrão self-hosted no mundo Java.

## Onde ficamos e por quê

**Opção A.** `identity` é uma coluna escolhida para desenvolver, então construímos o fluxo. A
costura account/credential (referência por ID entre contextos) preserva o caminho para mudar de
rumo depois sem reescrever:

- adicionar **refresh** (rotação + reuse detection + revogação) → provável módulo `auth` dedicado;
- adicionar **social login** → somar `spring-security-oauth2-client` + account linking, sem trocar
  de arquitetura.

Nota de segurança do refresh (para quando entrar, na opção A): refresh costuma ser **opaco** (não
JWT) e guardado **com hash**; cada uso **rotaciona** (emite novo, invalida o antigo); se um refresh
já rotacionado reaparece, é sinal de roubo → **revoga a família** daquele login. Access curto,
refresh longo e revogável.
