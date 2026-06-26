package com.caioms.java_marketplace.identity;

import com.caioms.java_marketplace.identity.dto.RegisterRequest;
import com.caioms.java_marketplace.identity.dto.RegisterResponse;
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

  private final UserService userService;

  @PostMapping("/register")
  public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
    var user = userService.register(request.email(), request.password(), request.role());

    var body = new RegisterResponse(user.getId(), user.getEmail(), user.getRole());
    return ResponseEntity.status(HttpStatus.CREATED).body(body);
  }
}
