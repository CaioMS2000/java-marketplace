package com.caioms.java_marketplace.modules.catalog.application.dto;

import com.caioms.java_marketplace.core.domain.vo.Money;
import com.caioms.java_marketplace.modules.catalog.application.models.Category;
import java.util.Set;
import java.util.UUID;

public record ProductDTO(UUID id, String name, Money price, String description,
        Set<Category> categories) {

}
