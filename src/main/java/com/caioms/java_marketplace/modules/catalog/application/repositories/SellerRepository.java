package com.caioms.java_marketplace.modules.catalog.application.repositories;

import com.caioms.java_marketplace.modules.catalog.application.models.Seller;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerRepository extends JpaRepository<Seller, UUID> {
	Optional<Seller> findById(UUID id);
}
