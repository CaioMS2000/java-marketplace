package com.caioms.java_marketplace.modules.identity.infrastructure.http.dto;

import com.caioms.java_marketplace.modules.identity.application.models.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(@NotBlank @Email String email,
        @NotBlank @Size(min = 6, max = 100) String password, @NotNull Role role) {
}
