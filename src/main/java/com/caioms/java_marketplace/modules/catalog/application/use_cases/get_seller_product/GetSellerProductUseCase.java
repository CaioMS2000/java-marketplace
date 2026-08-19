package com.caioms.java_marketplace.modules.catalog.application.use_cases.get_seller_product;

import com.caioms.java_marketplace.modules.catalog.application.dto.ProductDTO;
import com.caioms.java_marketplace.modules.catalog.application.errors.CatalogModuleError;
import com.caioms.java_marketplace.modules.catalog.application.errors.ProductNotFound;
import com.caioms.java_marketplace.modules.catalog.application.errors.SellerNotFound;
import com.caioms.java_marketplace.modules.catalog.application.repositories.ProductRepository;
import com.caioms.java_marketplace.modules.catalog.application.repositories.SellerRepository;
import io.vavr.control.Either;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetSellerProductUseCase {
	private final SellerRepository sellerRepository;
	private final ProductRepository productRepository;

	Either<CatalogModuleError, Result> execute(Params params) {
		var seller = sellerRepository.findById(params.sellerId);

		if (seller.isEmpty()) {
			return Either.left(new SellerNotFound(params.sellerId));
		}

		var product = productRepository.findById(params.productId);

		if (product.isEmpty()) {
			return Either.left(new ProductNotFound(params.productId));
		}

		var p = product.get();

		return Either.right(new Result(new ProductDTO(p.getId(), p.getName(), p.getPrice(),
		        p.getDescription(), p.getCategories())));
	}

	public record Params(UUID sellerId, UUID productId) {
	}

	public record Result(ProductDTO product) {
	}
}
