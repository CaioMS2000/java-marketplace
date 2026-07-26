package com.caioms.java_marketplace.modules.identity.application.use_cases.register.error;

import com.caioms.java_marketplace.modules.identity.application.errors.IdentityModuleError;

public sealed interface RegisterUserError extends IdentityModuleError
        permits EmailAlreadyInUse, AdminSelfRegistration {

}
