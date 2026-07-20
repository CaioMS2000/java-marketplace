package com.caioms.java_marketplace.modules.identity.application.use_cases.register_user;

import com.caioms.java_marketplace.modules.identity.application.models.Role;
import java.util.UUID;

public record RegisterUserResult(UUID id, String email, Role role) {}
