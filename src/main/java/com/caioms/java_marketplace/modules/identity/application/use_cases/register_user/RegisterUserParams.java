package com.caioms.java_marketplace.modules.identity.application.use_cases.register_user;

import com.caioms.java_marketplace.modules.identity.application.models.Role;

public record RegisterUserParams(String email, String password, Role role) {}
