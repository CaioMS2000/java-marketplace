package com.caioms.java_marketplace.modules.identity.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.caioms.java_marketplace.TestcontainersConfiguration;
import com.caioms.java_marketplace.modules.identity.application.models.CredentialType;
import com.caioms.java_marketplace.modules.identity.application.models.Role;
import com.caioms.java_marketplace.modules.identity.application.repositories.CredentialRepository;
import com.caioms.java_marketplace.modules.identity.application.repositories.UserRepository;
import com.caioms.java_marketplace.modules.identity.infrastructure.http.dto.RegisterUserRequest;
import com.caioms.java_marketplace.modules.identity.infrastructure.http.dto.RegisterUserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AuthRegisterIntegrationTest {

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

	@Test
	void registersUserWithUserRoleAndHashedPasswordCredential() {
		var request = new RegisterUserRequest("alice@example.com", "secret123");

		var response = client().post().uri("/auth/register").contentType(MediaType.APPLICATION_JSON)
		        .body(request).retrieve().toEntity(RegisterUserResponse.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().id()).isNotNull();
		assertThat(response.getBody().email()).isEqualTo("alice@example.com");
		assertThat(response.getBody().roles()).containsExactly(Role.USER);

		var user = userRepository.findByEmail("alice@example.com").orElseThrow();
		var credentials = credentialRepository.findByUserId(user.getId());
		assertThat(credentials).hasSize(1);

		var credential = credentials.get(0);
		assertThat(credential.getType()).isEqualTo(CredentialType.PASSWORD);
		assertThat(credential.getSubject()).isNotEqualTo("secret123");
		assertThat(credential.getSubject()).startsWith("$2");
	}
}
