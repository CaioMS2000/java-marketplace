# ADR-0005 — Formatação por tab com Eclipse JDT (XML único IDE + build)

- **Status:** Aceito
- **Data:** 2026-07-20
- **Decisores:** Caio Marques Silva
- **Contexto:** formatação de código Java, no build e na IDE (revisita o formatador mencionado de passagem em [ADR-0003](0003-organizacao-de-pacotes-e-convencao-de-nomes.md))

## Contexto

O projeto vinha com **google-java-format** no build (via Spotless) e, no VS Code, o formatador do
**Red Hat** (motor Eclipse JDT) aproximando o estilo Google via XML. Ou seja, **dois motores**
diferentes mirando o mesmo estilo — que concordam em ~95% e divergem em casos de borda (wrapping,
imports). O `mvn spotless:apply` era o árbitro final.

Surgiu a preferência por **indentação com tab** (alinhando com outro projeto do autor). Isso
esbarra num fato do google-java-format: ele é **deliberadamente não-configurável** — sempre 2
espaços, sem opção de tab. Logo, "usar tab" não é ajuste de config: exige **trocar o formatador**.

O formatador que aceita tab, é configurável e já estava em uso na IDE é o **Eclipse JDT**. Trocar
para ele no build também unifica os dois lados no mesmo motor.

## Decisão

1. **Indentação por tab.** Um tab por nível de indentação de início de linha; alinhamento de
   continuação usa espaço (`use_tabs_only_for_leading_indentations=true`).

2. **Eclipse JDT como formatador único**, nos dois lados:
   - Build: Spotless passo `<eclipse>` (substitui `<googleJavaFormat/>`).
   - IDE: Red Hat "Language Support for Java" (já usa o motor JDT).

3. **Um XML compartilhado e versionado:** `.config/eclipse-formatter.xml`. O Spotless o referencia
   por `<file>`; o VS Code por `java.format.settings.url`. Perfil próprio (nome `JavaMarketplace`),
   base tab, largura 100, indentação de switch — **não derivado do estilo Google**. As chaves não
   especificadas herdam os defaults do JDT.

4. **Config no repositório, não em URL remota.** Motivos: (a) o Eclipse JDT não publica um XML de
   perfil canônico para referenciar (o default é código dentro do jar); (b) este perfil é uma
   **decisão do projeto**, customizada — não um artefato de terceiro; (c) reprodutibilidade: config
   versionada e pinada, sem depender de rede/host externo, mudando só via commit junto do diff que
   afeta; (d) o Spotless exige um arquivo local de qualquer forma, então o arquivo é o denominador
   comum que mantém IDE e build na **mesma** fonte.

5. Mantidos os passos `removeUnusedImports` e `formatAnnotations` do Spotless.

Isto **supersede apenas a menção incidental** ao google-java-format no [ADR-0003](0003-organizacao-de-pacotes-e-convencao-de-nomes.md)
(que o citava como o formatador ativo, ao argumentar que a convenção de nomes não quebraria o
build). A substância do ADR-0003 (organização de pacote e naming) permanece intacta — e continua
verdadeira: o Eclipse JDT, como o google-java-format, formata espaçamento mas **não renomeia**
identificador nem segmento de pacote.

## Consequências

### Positivas

- **Um motor só, uma fonte de verdade.** IDE e build usam o mesmo JDT lendo o mesmo XML; some a
  divergência "google no build vs Eclipse aproximado na IDE".
- Indentação por tab, conforme a preferência.
- Config versionada, revisável em PR e pinada — a regra de formatação viaja com o código e muda só
  por commit.
- Quem clonar formata idêntico, sem setup manual da IDE além de apontar para o XML.

### Negativas / tradeoffs

- **Abandona o google-java-format**, que é o padrão *reconhecido* da indústria — um sinal de
  portfólio mais forte que um perfil tab customizado. Escolha consciente de preferência sobre
  convenção externa (mesma régua do [ADR-0003](0003-organizacao-de-pacotes-e-convencao-de-nomes.md)).
- **Tab vs espaço é preferência divisiva**; um revisor pode discordar. Documentado aqui como
  escolha, não descuido.
- **Perfil parcial → herda defaults do JDT** para chaves não setadas; o estilo pode mudar
  sutilmente entre versões do JDT que o Spotless empacota. Mitigável fixando a versão do passo
  `<eclipse>` ou expandindo o XML se surgir surpresa.
- **Sem ordenação automática de import.** O google-java-format ordenava imports; o formatador do
  JDT não reordena. Hoje só há `removeUnusedImports` (remove os não usados), sem impor ordem.

## Alternativas consideradas

**Manter google-java-format — rejeitado.** Não emite tab de forma alguma (não-configurável por
design); incompatível com o requisito.

**`indentWithTabs` do Spotless sobre o google-java-format — rejeitado.** Conflita: o google emite
espaço inclusive para alinhamento de continuação; converter só a indentação de início a tab
quebraria alinhamentos.

**Palantir Java Format — rejeitado.** Também opinativo e baseado em espaço; não resolve o tab.

**Apontar para uma URL remota (como se fez com o estilo Google) — rejeitado.** Não existe XML
canônico do Eclipse hospedado para referenciar; e como o perfil é do projeto, hospedá-lo fora só
para baixá-lo de volta adiciona dependência de rede e um segundo lugar para manter.

## Gatilho de revisão

Reconsiderar quando: (a) herdar defaults do JDT causar mudança de estilo indesejada entre
upgrades — aí fixar a versão do passo `<eclipse>` e/ou expandir o XML; (b) a falta de ordenação de
import incomodar — adicionar um passo `importOrder` no Spotless; ou (c) o projeto ganhar mais
autores e o padrão reconhecido (google-java-format) passar a valer mais que a preferência por tab.
