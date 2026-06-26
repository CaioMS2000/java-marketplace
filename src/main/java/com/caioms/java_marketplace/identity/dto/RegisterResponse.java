package com.caioms.java_marketplace.identity.dto;

import com.caioms.java_marketplace.identity.Role;
import java.util.UUID;

public record RegisterResponse(UUID id, String email, Role role) {}
