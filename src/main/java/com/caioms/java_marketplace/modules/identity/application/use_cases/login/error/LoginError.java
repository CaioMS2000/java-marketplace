package com.caioms.java_marketplace.modules.identity.application.use_cases.login.error;

import com.caioms.java_marketplace.modules.identity.application.errors.IdentityModuleError;

public sealed interface LoginError extends IdentityModuleError permits InvalidCredentials {
}
