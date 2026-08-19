package com.caioms.java_marketplace.modules.catalog.application.repositories;

import com.caioms.java_marketplace.modules.catalog.application.models.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID> {
	Optional<Product> findById(UUID id);

	List<Product> findBySellerId(UUID sellerId);

	@Query(value = """
	        SELECT * FROM products p
	        WHERE (:categoryId IS NULL OR EXISTS (
	                SELECT 1 FROM product_categories pc
	                WHERE pc.product_id = p.id AND pc.category_id = :categoryId))
	          AND (:minPrice IS NULL OR p.price_amount >= :minPrice)
	          AND (:maxPrice IS NULL OR p.price_amount <= :maxPrice)
	          AND (:search IS NULL OR p.search_vector @@ plainto_tsquery('portuguese', :search))
	        """, countQuery = """
	        SELECT count(*) FROM products p
	        WHERE (:categoryId IS NULL OR EXISTS (
	                SELECT 1 FROM product_categories pc
	                WHERE pc.product_id = p.id AND pc.category_id = :categoryId))
	          AND (:minPrice IS NULL OR p.price_amount >= :minPrice)
	          AND (:maxPrice IS NULL OR p.price_amount <= :maxPrice)
	          AND (:search IS NULL OR p.search_vector @@ plainto_tsquery('portuguese', :search))
	        """, nativeQuery = true)
	Page<Product> search(@Param("categoryId") UUID categoryId,
	        @Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice,
	        @Param("search") String search, Pageable pageable);
}
