package com.caioms.java_marketplace.modules.identity.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.caioms.java_marketplace.TestcontainersConfiguration;
import com.caioms.java_marketplace.modules.identity.application.models.Role;
import com.caioms.java_marketplace.modules.identity.application.repositories.UserRepository;
import com.caioms.java_marketplace.modules.identity.infrastructure.http.dto.RegisterUserRequest;
import com.caioms.java_marketplace.modules.identity.infrastructure.http.dto.RegisterUserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AuthRegisterIntegrationTest {

  @LocalServerPort private int port;

  @Autowired private UserRepository userRepository;

  private RestClient client() {
    return RestClient.create("http://localhost:" + port);
  }

  @Test
  void registersUserAndPersistsHashedPassword() {
    var request = new RegisterUserRequest("alice@example.com", "secret123", Role.USER);

    var response =
        client()
            .post()
            .uri("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .toEntity(RegisterUserResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().id()).isNotNull();
    assertThat(response.getBody().email()).isEqualTo("alice@example.com");
    assertThat(response.getBody().role()).isEqualTo(Role.USER);

    var persisted = userRepository.findAll();
    assertThat(persisted).hasSize(1);

    var user = persisted.get(0);
    assertThat(user.getEmail()).isEqualTo("alice@example.com");
    assertThat(user.getPasswordHash()).isNotEqualTo("secret123");
    assertThat(user.getPasswordHash()).startsWith("$2");
  }

  @Test
  void rejectsAdminSelfRegistration() {
    var request = new RegisterUserRequest("eve@example.com", "secret123", Role.ADMIN);

    assertThatThrownBy(
            () ->
                client()
                    .post()
                    .uri("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity())
        .isInstanceOf(HttpClientErrorException.class)
        .extracting(ex -> ((HttpClientErrorException) ex).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);

    assertThat(userRepository.count()).isZero();
  }
}
