package com.caioms.java_marketplace.modules.identity.infrastructure.http;

import com.caioms.java_marketplace.modules.identity.application.use_cases.login.LoginUseCase;
import com.caioms.java_marketplace.modules.identity.application.use_cases.login.error.InvalidCredentials;
import com.caioms.java_marketplace.modules.identity.application.use_cases.register.RegisterUserUseCase;
import com.caioms.java_marketplace.modules.identity.application.use_cases.register.error.AdminSelfRegistration;
import com.caioms.java_marketplace.modules.identity.application.use_cases.register.error.EmailAlreadyInUse;
import com.caioms.java_marketplace.modules.identity.infrastructure.http.dto.LoginRequest;
import com.caioms.java_marketplace.modules.identity.infrastructure.http.dto.LoginResponse;
import com.caioms.java_marketplace.modules.identity.infrastructure.http.dto.RegisterUserRequest;
import com.caioms.java_marketplace.modules.identity.infrastructure.http.dto.RegisterUserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final RegisterUserUseCase registerUser;
	private final LoginUseCase login;

	@PostMapping("/register")
	public ResponseEntity<Object> register(@Valid @RequestBody RegisterUserRequest request) {
		var result = registerUser.execute(new RegisterUserUseCase.Params(request.email(),
		        request.password(), request.role()));

		return result.fold(error -> switch (error) {
			case AdminSelfRegistration a -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
			        .body("Papel ADMIN não pode ser auto-atribuído no registro");
			case EmailAlreadyInUse e ->
			    ResponseEntity.status(HttpStatus.CONFLICT).body("E-mail já cadastrado");
		}, success -> ResponseEntity.status(HttpStatus.CREATED)
		        .body(new RegisterUserResponse(success.id(), success.email(), success.role())));
	}

	@PostMapping("/login")
	public ResponseEntity<Object> login(@Valid @RequestBody LoginRequest request) {
		var result = login.execute(new LoginUseCase.Params(request.email(), request.password()));

		return result.fold(error -> switch (error) {
			case InvalidCredentials i ->
			    ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciais inválidas");
		}, success -> ResponseEntity.ok(new LoginResponse(success.accessToken(), "Bearer")));
	}
}
