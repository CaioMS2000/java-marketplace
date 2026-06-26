package com.caioms.java_marketplace.shared.security;

import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

  @Bean
  public Algorithm jwtAlgorithm(@Value("${security.token.secret}") String secret) {
    return Algorithm.HMAC256(secret);
  }
}
