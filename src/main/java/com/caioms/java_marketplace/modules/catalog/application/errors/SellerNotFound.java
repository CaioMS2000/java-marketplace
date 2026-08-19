package com.caioms.java_marketplace.modules.catalog.application.errors;

import java.util.UUID;

public record SellerNotFound(UUID sellerId) implements CatalogModuleError {

}
