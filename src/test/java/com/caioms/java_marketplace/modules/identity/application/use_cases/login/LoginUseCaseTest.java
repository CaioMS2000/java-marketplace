package com.caioms.java_marketplace.modules.identity.application.use_cases.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.caioms.java_marketplace.modules.identity.application.jwt.AccessTokenIssuer;
import com.caioms.java_marketplace.modules.identity.application.models.Credential;
import com.caioms.java_marketplace.modules.identity.application.models.CredentialType;
import com.caioms.java_marketplace.modules.identity.application.models.Role;
import com.caioms.java_marketplace.modules.identity.application.models.User;
import com.caioms.java_marketplace.modules.identity.application.repositories.CredentialRepository;
import com.caioms.java_marketplace.modules.identity.application.repositories.UserRepository;
import com.caioms.java_marketplace.modules.identity.application.use_cases.login.error.InvalidCredentials;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private CredentialRepository credentialRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private AccessTokenIssuer accessTokenIssuer;
	@InjectMocks
	private LoginUseCase login;

	@Test
	void retornaRightComToken_quandoCredenciaisValidas() {
		var user = new User("alice@example.com", Set.of(Role.USER));
		var credential = Credential.password(user, "HASHED");
		when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
		when(credentialRepository.findByUserIdAndType(any(), eq(CredentialType.PASSWORD)))
		        .thenReturn(Optional.of(credential));
		when(passwordEncoder.matches("secret123", "HASHED")).thenReturn(true);
		when(accessTokenIssuer.issue(any(), eq(Set.of(Role.USER)))).thenReturn("access-token");

		var result = login.execute(new LoginUseCase.Params("alice@example.com", "secret123"));

		assertThat(result.isRight()).isTrue();
		assertThat(result.get().accessToken()).isEqualTo("access-token");
	}

	@Test
	void retornaLeftInvalidCredentials_quandoEmailInexistente() {
		when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

		var result = login.execute(new LoginUseCase.Params("ghost@example.com", "secret123"));

		assertThat(result.isLeft()).isTrue();
		assertThat(result.getLeft()).isInstanceOf(InvalidCredentials.class);
		verifyNoInteractions(accessTokenIssuer);
	}

	@Test
	void retornaLeftInvalidCredentials_quandoSenhaErrada() {
		var user = new User("bob@example.com", Set.of(Role.USER));
		var credential = Credential.password(user, "HASHED");
		when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(user));
		when(credentialRepository.findByUserIdAndType(any(), eq(CredentialType.PASSWORD)))
		        .thenReturn(Optional.of(credential));
		when(passwordEncoder.matches("wrongpass", "HASHED")).thenReturn(false);

		var result = login.execute(new LoginUseCase.Params("bob@example.com", "wrongpass"));

		assertThat(result.isLeft()).isTrue();
		assertThat(result.getLeft()).isInstanceOf(InvalidCredentials.class);
		verifyNoInteractions(accessTokenIssuer);
	}
}
