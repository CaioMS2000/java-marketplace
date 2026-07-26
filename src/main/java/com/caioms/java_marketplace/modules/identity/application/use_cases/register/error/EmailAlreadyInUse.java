package com.caioms.java_marketplace.modules.identity.application.use_cases.register.error;

public record EmailAlreadyInUse(String email) implements RegisterUserError {

}
