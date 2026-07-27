package com.caioms.java_marketplace.modules.identity.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.caioms.java_marketplace.TestcontainersConfiguration;
import com.caioms.java_marketplace.modules.identity.application.repositories.CredentialRepository;
import com.caioms.java_marketplace.modules.identity.application.repositories.UserRepository;
import com.caioms.java_marketplace.modules.identity.infrastructure.http.dto.LoginRequest;
import com.caioms.java_marketplace.modules.identity.infrastructure.http.dto.LoginResponse;
import com.caioms.java_marketplace.modules.identity.infrastructure.http.dto.RegisterUserRequest;
import org.junit.jupiter.api.BeforeEach;
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
class AuthLoginIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private CredentialRepository credentialRepository;

	@BeforeEach
	void clean() {
		credentialRepository.deleteAll();
		userRepository.deleteAll();
	}

	private RestClient client() {
		return RestClient.create("http://localhost:" + port);
	}

	private void register(String email, String password) {
		client().post().uri("/auth/register").contentType(MediaType.APPLICATION_JSON)
		        .body(new RegisterUserRequest(email, password)).retrieve().toBodilessEntity();
	}

	@Test
	void loginComCredenciaisValidas_retornaAccessToken() {
		register("alice@example.com", "secret123");

		var response = client().post().uri("/auth/login").contentType(MediaType.APPLICATION_JSON)
		        .body(new LoginRequest("alice@example.com", "secret123")).retrieve()
		        .toEntity(LoginResponse.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().accessToken()).isNotBlank();
		assertThat(response.getBody().tokenType()).isEqualTo("Bearer");
	}

	@Test
	void loginComSenhaErrada_retorna401() {
		register("bob@example.com", "secret123");

		assertThatThrownBy(
		        () -> client().post().uri("/auth/login").contentType(MediaType.APPLICATION_JSON)
		                .body(new LoginRequest("bob@example.com", "wrongpass")).retrieve()
		                .toBodilessEntity())
		        .isInstanceOf(HttpClientErrorException.class)
		        .extracting(ex -> ((HttpClientErrorException) ex).getStatusCode())
		        .isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void loginComEmailDesconhecido_retorna401() {
		assertThatThrownBy(
		        () -> client().post().uri("/auth/login").contentType(MediaType.APPLICATION_JSON)
		                .body(new LoginRequest("ghost@example.com", "secret123")).retrieve()
		                .toBodilessEntity())
		        .isInstanceOf(HttpClientErrorException.class)
		        .extracting(ex -> ((HttpClientErrorException) ex).getStatusCode())
		        .isEqualTo(HttpStatus.UNAUTHORIZED);
	}
}
