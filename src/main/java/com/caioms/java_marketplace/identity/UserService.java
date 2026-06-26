package com.caioms.java_marketplace.identity;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public User register(String email, String rawPassword, Role role) {
    if (role == Role.ADMIN) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Papel ADMIN não pode ser auto-atribuído no registro");
    }
    if (userRepository.existsByEmail(email)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
    }

    var user = new User(email, passwordEncoder.encode(rawPassword), role);
    return userRepository.save(user);
  }
}
