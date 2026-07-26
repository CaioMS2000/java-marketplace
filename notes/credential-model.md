# Type-token: discriminação em compile-time para factories (NÃO é entidade JPA)

Padrão de tipagem em que um parâmetro-token (`CredentialType<T>`) discrimina e **força, no
compilador, o tipo do outro parâmetro** (`T data`) — a versão Java mais próxima de "escolheu
PASSWORD → tem que dar os dados de PASSWORD".

> **Caveat — isto NÃO é a entidade `Credential` persistida.** É um shape de *fábrica em memória*,
> não serve como modelo JPA:
> - `Object data` e o discriminador ser um token (objeto Java, não coluna) → o Hibernate não tem
>   como mapear pra colunas; não há `@Entity`/`@Id`/`@Column`/tabela `credentials`.
> - `type()` devolve o token, não o enum (`PASSWORD`/`GOOGLE`) que vive na coluna `type`.
> - `Object data` perde o tipo no banco — exigiria colunas por tipo ou JSON (herança/mapping).
>
> A entidade real (`Credential` com linha plana + factory `Credential.password(user, hash)`) **já
> dá a mesma garantia** de "não dá pra criar um PASSWORD sem hash", e é persistível. Se um dia
> quiser discriminação forte *no tipo* COM persistência, o caminho JPA é herança
> (`@Inheritance` SINGLE_TABLE + coluna discriminadora) + hierarquia `sealed` — não `Object data`.
> Guardar isto só como referência do **truque de tipagem**.

```java
package com.caioms.java_marketplace.modules.identity.application.models;


public class Credential {

	private final CredentialType<?> type;
	private final Object data;

	private Credential(CredentialType<?> type, Object data) {
		this.type = type;
		this.data = data;
	}

	// O <T> do token discrimina e força o tipo do 'data'.
	public static <T> Credential create(CredentialType<T> type, T data) {
		return new Credential(type, data);
	}

	public CredentialType<?> type() {
		return type;
	}

	public Object data() {
		return data;
	}

	/** Tokens tipados: cada constante fixa o T. */
	public static final class CredentialType<T> {
		public static final CredentialType<PasswordData> PASSWORD = new CredentialType<>();
		public static final CredentialType<OAuthData> GOOGLE = new CredentialType<>();

		private CredentialType() {
		}
	}

	public record PasswordData(User user, String hash) {
	}

	public record OAuthData(User user, String provider, String providerSubject) {
	}

	// Prova do ponto (chame de qualquer lugar):
	//
	// Credential.create(CredentialType.PASSWORD, new PasswordData(user, "hash")); // compila
	// Credential.create(CredentialType.GOOGLE, new OAuthData(user, "g", "sub")); // compila
	//
	// A linha abaixo NÃO compila — o token PASSWORD exige PasswordData, não OAuthData:
	//
	// Credential.create(CredentialType.PASSWORD, new OAuthData(user, "g", "sub")); // erro de
	// compilação
}
```
