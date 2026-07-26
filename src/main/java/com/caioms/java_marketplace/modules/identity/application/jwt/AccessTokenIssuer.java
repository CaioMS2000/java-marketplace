package com.caioms.java_marketplace.modules.identity.application.jwt;

import com.caioms.java_marketplace.http.security.JWTProvider;
import com.caioms.java_marketplace.modules.identity.application.models.Role;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Emissão do access token: onde o identity decide o conteúdo do token (subject + papéis) e delega a
 * assinatura à primitiva transversal {@link JWTProvider} (ver ADR-0002). Mantém a mecânica de JWT
 * (issuer, TTL, claims) fora do use case.
 */
@Component
public class AccessTokenIssuer {

	private final JWTProvider jwtProvider;
	private final String issuer;
	private final Duration accessTtl;

	public AccessTokenIssuer(JWTProvider jwtProvider,
	        @Value("${security.token.issuer:java-marketplace}") String issuer,
	        @Value("${security.token.access-ttl:PT15M}") Duration accessTtl) {
		this.jwtProvider = jwtProvider;
		this.issuer = issuer;
		this.accessTtl = accessTtl;
	}

	public String issue(UUID userId, Role role) {
		var claims = List.of(new JWTProvider.ClaimData("roles", List.of(role.name())));
		var params = new JWTProvider.GenerateTokenParams(issuer, claims, userId.toString(),
		        Instant.now().plus(accessTtl));
		return jwtProvider.generateJwtToken(params);
	}
}
