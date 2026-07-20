package com.caioms.java_marketplace.modules.identity.application.use_cases.register_user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.caioms.java_marketplace.modules.identity.application.models.Role;
import com.caioms.java_marketplace.modules.identity.application.models.User;
import com.caioms.java_marketplace.modules.identity.application.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@InjectMocks
	private RegisterUserUseCase registerUser;

	@Test
	void retornaRightComResultadoEHasheiaSenha_quandoDadosValidos() {
		var params = new RegisterUserParams("alice@example.com", "secret123", Role.USER);
		when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
		when(passwordEncoder.encode("secret123")).thenReturn("HASHED");
		when(userRepository.save(any(User.class)))
		        .thenAnswer(invocation -> invocation.getArgument(0));

		var result = registerUser.execute(params);

		assertThat(result.isRight()).isTrue();
		assertThat(result.get().email()).isEqualTo("alice@example.com");
		assertThat(result.get().role()).isEqualTo(Role.USER);

		var savedUser = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(savedUser.capture());
		assertThat(savedUser.getValue().getPasswordHash()).isEqualTo("HASHED");
		assertThat(savedUser.getValue().getPasswordHash()).isNotEqualTo("secret123");
	}

	@Test
	void retornaLeftAdminSelfRegistration_quandoRoleAdmin() {
		var params = new RegisterUserParams("eve@example.com", "secret123", Role.ADMIN);

		var result = registerUser.execute(params);

		assertThat(result.isLeft()).isTrue();
		assertThat(result.getLeft()).isInstanceOf(RegisterUserError.AdminSelfRegistration.class);
		verifyNoInteractions(userRepository, passwordEncoder);
	}

	@Test
	void retornaLeftEmailAlreadyInUse_quandoEmailJaExiste() {
		var params = new RegisterUserParams("bob@example.com", "secret123", Role.USER);
		when(userRepository.existsByEmail("bob@example.com")).thenReturn(true);

		var result = registerUser.execute(params);

		assertThat(result.isLeft()).isTrue();
		assertThat(result.getLeft()).isInstanceOfSatisfying(
		        RegisterUserError.EmailAlreadyInUse.class,
		        error -> assertThat(error.email()).isEqualTo("bob@example.com"));
		verify(userRepository, never()).save(any(User.class));
		verifyNoInteractions(passwordEncoder);
	}
}
