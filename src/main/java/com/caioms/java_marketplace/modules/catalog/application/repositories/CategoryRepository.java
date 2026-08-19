package com.caioms.java_marketplace.modules.catalog.application.repositories;

import com.caioms.java_marketplace.modules.catalog.application.models.Category;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

}
