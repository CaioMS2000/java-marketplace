package com.caioms.java_marketplace.modules.identity.infrastructure.http.dto;

import com.caioms.java_marketplace.modules.identity.application.models.Role;
import java.util.Set;
import java.util.UUID;

public record RegisterUserResponse(UUID id, String email, Set<Role> roles) {
}
