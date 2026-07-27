package com.caioms.java_marketplace.modules.identity.application.use_cases.login;

import com.caioms.java_marketplace.modules.identity.application.jwt.AccessTokenIssuer;
import com.caioms.java_marketplace.modules.identity.application.models.CredentialType;
import com.caioms.java_marketplace.modules.identity.application.repositories.CredentialRepository;
import com.caioms.java_marketplace.modules.identity.application.repositories.UserRepository;
import com.caioms.java_marketplace.modules.identity.application.use_cases.login.error.InvalidCredentials;
import com.caioms.java_marketplace.modules.identity.application.use_cases.login.error.LoginError;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginUseCase {

	private final UserRepository userRepository;
	private final CredentialRepository credentialRepository;
	private final PasswordEncoder passwordEncoder;
	private final AccessTokenIssuer accessTokenIssuer;

	public Either<LoginError, Result> execute(Params params) {
		var userOpt = userRepository.findByEmail(params.email());
		if (userOpt.isEmpty()) {
			return Either.left(new InvalidCredentials());
		}
		var user = userOpt.get();

		var credentialOpt = credentialRepository.findByUserIdAndType(user.getId(),
		        CredentialType.PASSWORD);
		if (credentialOpt.isEmpty()
		        || !passwordEncoder.matches(params.password(), credentialOpt.get().getSubject())) {
			return Either.left(new InvalidCredentials());
		}

		var accessToken = accessTokenIssuer.issue(user.getId(), user.getRoles());
		return Either.right(new Result(accessToken));
	}

	public record Params(String email, String password) {
	}

	public record Result(String accessToken) {
	}
}
