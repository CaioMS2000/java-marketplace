package com.caioms.java_marketplace.modules.identity.application.use_cases.register_user;

import com.caioms.java_marketplace.modules.identity.application.models.Role;
import com.caioms.java_marketplace.modules.identity.application.models.User;
import com.caioms.java_marketplace.modules.identity.application.repositories.UserRepository;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterUserUseCase {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public Either<RegisterUserError, RegisterUserResult> execute(RegisterUserParams params) {
		if (params.role() == Role.ADMIN) {
			return Either.left(new RegisterUserError.AdminSelfRegistration());
		}
		if (userRepository.existsByEmail(params.email())) {
			return Either.left(new RegisterUserError.EmailAlreadyInUse(params.email()));
		}

		var user = new User(params.email(), passwordEncoder.encode(params.password()),
		        params.role());
		var saved = userRepository.save(user);
		return Either
		        .right(new RegisterUserResult(saved.getId(), saved.getEmail(), saved.getRole()));
	}
}
