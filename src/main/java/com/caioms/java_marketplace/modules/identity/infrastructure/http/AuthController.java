package com.caioms.java_marketplace.modules.identity.infrastructure.http;

import com.caioms.java_marketplace.modules.identity.application.use_cases.register_user.RegisterUserError;
import com.caioms.java_marketplace.modules.identity.application.use_cases.register_user.RegisterUserParams;
import com.caioms.java_marketplace.modules.identity.application.use_cases.register_user.RegisterUserUseCase;
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

  @PostMapping("/register")
  public ResponseEntity<Object> register(@Valid @RequestBody RegisterUserRequest request) {
    var result =
        registerUser.execute(
            new RegisterUserParams(request.email(), request.password(), request.role()));

    return result.fold(
        error ->
            switch (error) {
              case RegisterUserError.AdminSelfRegistration a ->
                  ResponseEntity.status(HttpStatus.BAD_REQUEST)
                      .body("Papel ADMIN não pode ser auto-atribuído no registro");
              case RegisterUserError.EmailAlreadyInUse e ->
                  ResponseEntity.status(HttpStatus.CONFLICT).body("E-mail já cadastrado");
            },
        success ->
            ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterUserResponse(success.id(), success.email(), success.role())));
  }
}
