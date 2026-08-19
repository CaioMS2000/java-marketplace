# Fluxo cross-module — quem contém o quê

Nota de referência (não é ADR): como decidir **em qual módulo mora um fluxo que atravessa dois
contextos**, usando o onboarding de seller como caso concreto (`User` vive em `identity`, `Seller`
vive em `catalog`). Serve para quando o cadastro de seller / concessão do papel `SELLER` entrar em
pauta. Relaciona-se com a regra de referência por ID entre módulos
([ADR-0001](../adr/0001-monolito-modular.md)) e com o gatilho de módulo `auth` dedicado
([ADR-0002](../adr/0002-seguranca-no-shared-emissao-no-identity.md)).

## A regra que decide tudo: a seta de dependência

`catalog` referencia `userId` → **catalog → identity**. Identity é upstream, catalog é downstream.

- Catalog conhece identity; **identity NÃO conhece catalog.**
- Essa seta **não pode inverter** — inverter funde os dois módulos e mata a extração futura.

Consequência imediata: o fluxo de onboarding **não pode morar em identity**, porque ele precisa
criar um `Seller`, e identity não pode saber o que é `Seller`.

## Onde o fluxo mora

**No módulo dono do "esqueleto" do resultado.** "Virar seller" gira em torno da entidade `Seller`
→ o fluxo é do **catalog**. Fica um `RegisterSellerUseCase` (ou `SellerOnboardingUseCase`) que:

1. cria o `Seller` (dado dele, tabela dele — tranquilo);
2. concede o papel `SELLER` ao user — escrita no território de identity, então **não toca
   `users`/`user_roles` direto**. Chama uma **porta publicada** de identity.

```
catalog/application/seller/RegisterSellerUseCase
    └─ chama → identity.api.IdentityRoleService.grantRole(userId, Role.SELLER)
```

Identity publica uma interface pública pequena (`IdentityRoleService` / `IdentityApi`) e esconde o
resto. Catalog depende **da interface**, nunca das entities de identity. É a mesma disciplina de
"controller não chama repositório direto", elevada para entre módulos: **nunca se alcança as
entranhas do outro módulo, só a API que ele decidiu expor.**

## O ponto espinhoso: transação / consistência

Como ainda é **um banco só**, duas escolhas honestas:

- **Síncrono, uma transação** — cria Seller e chama `grantRole` no mesmo `@Transactional`. Simples
  e consistente já; custo: acopla os módulos no nível da transação (rollback cruza a fronteira).
- **Evento de domínio** — catalog publica `SellerRegistered`; identity escuta e concede o papel.
  Desacopla de verdade e é a costura que se quer quando os módulos virarem serviços; custo:
  consistência eventual (por um instante, seller sem o papel).

**Decisão atual (recomendação):** síncrono via porta publicada. Honesto, simples, história limpa
("catalog orquestra, identity expõe API, dependência num sentido só"). O evento fica como próximo
passo natural — candidato a ADR: *"hoje chamada síncrona por porta; quando surgir refresh/OAuth ou
split de serviço, migrar `grantRole` para evento `SellerRegistered`."*

## Resumo

- Fluxo cross-module mora em **quem é dono do esqueleto do resultado** (aqui, catalog).
- Comunicação sempre por **porta publicada** do módulo upstream, nunca tabela/entity direto.
- **Seta de dependência é sagrada:** catalog → identity, nunca o contrário.
- Consistência: síncrono numa transação agora; evento quando precisar desacoplar de verdade.
- Não construir a porta nem o evento hoje. Quando surgir "preciso mexer no user aqui do catalog",
  *essa* dor faz nascer a `IdentityRoleService`.
