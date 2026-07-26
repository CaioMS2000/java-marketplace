package com.caioms.java_marketplace.modules.identity.application.use_cases.register;

import com.caioms.java_marketplace.modules.identity.application.models.Credential;
import com.caioms.java_marketplace.modules.identity.application.models.Role;
import com.caioms.java_marketplace.modules.identity.application.models.User;
import com.caioms.java_marketplace.modules.identity.application.repositories.CredentialRepository;
import com.caioms.java_marketplace.modules.identity.application.repositories.UserRepository;
import com.caioms.java_marketplace.modules.identity.application.use_cases.register.error.AdminSelfRegistration;
import com.caioms.java_marketplace.modules.identity.application.use_cases.register.error.EmailAlreadyInUse;
import com.caioms.java_marketplace.modules.identity.application.use_cases.register.error.RegisterUserError;
import io.vavr.control.Either;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterUserUseCase {

	private final UserRepository userRepository;
	private final CredentialRepository credentialRepository;
	private final PasswordEncoder passwordEncoder;

	public Either<RegisterUserError, Result> execute(Params params) {
		if (params.role() == Role.ADMIN) {
			return Either.left(new AdminSelfRegistration());
		}
		if (userRepository.existsByEmail(params.email())) {
			return Either.left(new EmailAlreadyInUse(params.email()));
		}

		var password = passwordEncoder.encode(params.password());
		var user = new User(params.email(), params.role());
		var credential = Credential.password(user, password);
		var savedUser = userRepository.save(user);
		var __savedCredential = credentialRepository.save(credential);

		return Either
		        .right(new Result(savedUser.getId(), savedUser.getEmail(), savedUser.getRole()));
	}

	public record Params(String email, String password, Role role) {
	}

	public record Result(UUID id, String email, Role role) {
	}
}
