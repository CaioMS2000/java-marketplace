# Busca pública do catálogo — por que virou uma query nativa só

Nota de referência (não é ADR): registra por que `ProductRepository.search` ([repositories/ProductRepository.java](../src/main/java/com/caioms/java_marketplace/modules/catalog/application/repositories/ProductRepository.java))
acabou sendo uma única `@Query` nativa em vez de `Specification`/métodos derivados, para não redescobrir o
mesmo caminho da próxima vez que um filtro combinável precisar ser adicionado.

## O requisito: filtros combináveis, não excludentes

`ListPublicProductsUseCase` recebe categoria, faixa de preço e busca textual, todos opcionais e
usados **ao mesmo tempo** (o usuário filtra por categoria E ordena por preço E está na página 2).
Isso descarta de cara métodos derivados tipo `findByCategoryIdAndPriceBetween...`: cada combinação
de filtro presente/ausente exigiria um método novo.

## Primeira tentativa: `Specification`

`JpaSpecificationExecutor` é o caminho idiomático do Spring Data pra esse problema — cada filtro
vira um `Specification<Product>` que retorna `null` quando o parâmetro não veio, e
`Specification.allOf(...)` ignora os nulos. Funcionou bem para categoria (`EXISTS`/`join` via
Criteria) e faixa de preço (`cb.greaterThanOrEqualTo`/`cb.lessThanOrEqualTo`).

**Onde travou:** o filtro de busca textual usa `tsvector`/`@@`/`plainto_tsquery` (Postgres), e o
Criteria API do JPA não tem como expressar o operador `@@` de forma portável — não é uma função
chamável, é um operador infixo específico do dialeto. Forçar isso via `cb.function(...)` exigiria
registrar função customizada no dialect e ainda assim ficaria frágil/não testado.

## Solução: uma `@Query` nativa com bypass por `IS NULL`

Trocado por um único método nativo que já resolve os três filtros + paginação:

```sql
SELECT * FROM products p
WHERE (:categoryId IS NULL OR EXISTS (
        SELECT 1 FROM product_categories pc
        WHERE pc.product_id = p.id AND pc.category_id = :categoryId))
  AND (:minPrice IS NULL OR p.price_amount >= :minPrice)
  AND (:maxPrice IS NULL OR p.price_amount <= :maxPrice)
  AND (:search IS NULL OR p.search_vector @@ plainto_tsquery('portuguese', :search))
```

Dois detalhes que valem registrar:

- **Categoria via `EXISTS`, não `JOIN`.** `products` ↔ `categories` é N-N
  ([V5__product_categories_many_to_many.sql](../src/main/resources/db/migration/V5__product_categories_many_to_many.sql)).
  Um `JOIN product_categories` duplicaria a linha do produto se ele tivesse mais de uma categoria
  batendo no filtro. `EXISTS` evita isso sem precisar de `DISTINCT`.
- **`(:param IS NULL OR condição)`** é o padrão pra "filtro opcional" em SQL puro — cada condição
  vira efetivamente `true` quando o parâmetro não veio, sem precisar montar a query em partes no
  Java.

`countQuery` foi escrita à mão (mesmo `WHERE`, `count(*)` no lugar do `SELECT *`) porque native
query com paginação não infere a contagem sozinha.

Como consequência, `JpaSpecificationExecutor` foi removido do repositório — ficaria como código
morto, já que a única busca combinável do módulo migrou pra cá.

## Postgres não tem `FULLTEXT INDEX`

Detalhe à parte que rendeu confusão: `FULLTEXT INDEX` é sintaxe MySQL. O equivalente Postgres é
indexar uma coluna `tsvector` com GIN — usamos uma coluna gerada (`GENERATED ALWAYS AS ... STORED`)
em vez de um índice de expressão, porque materializar o `tsvector` uma vez na escrita evita
recalcular `to_tsvector(...)` em toda leitura e evita repetir a expressão exata em cada query
([V6__product_fulltext_search.sql](../src/main/resources/db/migration/V6__product_fulltext_search.sql)).
Trade-off: espaço em disco extra pela coluna + índice.

## Como validar SQL exótico antes de comprometer no Java

Antes de escrever a `@Query`, a sintaxe (`EXISTS`, `@@`, `plainto_tsquery`, bypass por `IS NULL`)
foi testada direto via `psql` contra o container Postgres local, dentro de uma transação com
`ROLLBACK` no final — não sujou o banco e confirmou os três cenários (só busca textual,
categoria+preço combinados, todos os filtros nulos) antes de existir uma linha de Java. Vale
repetir essa técnica sempre que a query for native/nativa o bastante pra não ter como confiar só na
compilação.

## Resumo

- Filtros combináveis (não excludentes) descartam métodos derivados de cara.
- `Specification` cobre bem filtros JPA-nativos (igualdade, ranges, joins), mas não operadores
  específicos de dialeto como `@@` — aí vira `@Query` nativa.
- N-N em filtro: `EXISTS`, não `JOIN`, pra não duplicar linha.
- `(:param IS NULL OR condição)` é o padrão pra filtro opcional em SQL puro.
- Postgres não tem `FULLTEXT INDEX`; é `tsvector` + índice GIN.
- SQL nativo vale testar direto no banco (transação + `ROLLBACK`) antes de virar `@Query` no Java.
