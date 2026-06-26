package com.caioms.java_marketplace.shared.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class JWTProvider {
  private final Algorithm algorithm;
  private final JWTVerifier verifier;

  public JWTProvider(Algorithm algorithm) {
    this.algorithm = algorithm;
    this.verifier = JWT.require(algorithm).build();
  }

  public Optional<DecodedJWT> validate(String token) {
    token = token.startsWith("Bearer ") ? token.substring(7) : token;
    try {
      return Optional.of(verifier.verify(token));
    } catch (JWTVerificationException e) {
      return Optional.empty();
    }
  }

  public String generateJwtToken(GenerateTokenParams params) {
    var builder =
        JWT.create()
            .withIssuer(params.issuer())
            .withExpiresAt(params.expiresAt())
            .withSubject(params.subject());

    params
        .claims()
        .forEach(
            claim -> {
              builder.withClaim(claim.name(), claim.values());
            });

    var token = builder.sign(algorithm);

    return token;
  }
}
