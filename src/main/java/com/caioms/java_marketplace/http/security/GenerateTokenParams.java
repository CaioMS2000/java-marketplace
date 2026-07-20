package com.caioms.java_marketplace.http.security;

import java.time.Instant;
import java.util.List;

record ClaimData(String name, List<?> values) {
}

public record GenerateTokenParams(String issuer, List<ClaimData> claims, String subject,
        Instant expiresAt) {
}
