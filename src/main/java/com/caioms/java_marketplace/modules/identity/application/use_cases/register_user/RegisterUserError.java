package com.caioms.java_marketplace.modules.identity.application.use_cases.register_user;

public sealed interface RegisterUserError {
  record AdminSelfRegistration() implements RegisterUserError {}

  record EmailAlreadyInUse(String email) implements RegisterUserError {}
}
