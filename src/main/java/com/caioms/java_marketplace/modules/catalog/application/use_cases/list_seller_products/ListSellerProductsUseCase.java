package com.caioms.java_marketplace.modules.catalog.application.use_cases.list_seller_products;

import com.caioms.java_marketplace.modules.catalog.application.dto.ProductDTO;
import com.caioms.java_marketplace.modules.catalog.application.errors.CatalogModuleError;
import com.caioms.java_marketplace.modules.catalog.application.errors.SellerNotFound;
import com.caioms.java_marketplace.modules.catalog.application.repositories.ProductRepository;
import com.caioms.java_marketplace.modules.catalog.application.repositories.SellerRepository;
import io.vavr.control.Either;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListSellerProductsUseCase {
	private final ProductRepository productRepository;
	private final SellerRepository sellerRepository;

	Either<CatalogModuleError, Result> execute(Params params) {
		var seller = sellerRepository.findById(params.sellerId);

		if (seller.isEmpty()) {
			return Either.left(new SellerNotFound(params.sellerId));
		}

		var products = productRepository.findBySellerId(params.sellerId).stream()
		        .map(product -> new ProductDTO(product.getId(), product.getName(),
		                product.getPrice(), product.getDescription(), product.getCategories()))
		        .toList();

		return Either.right(new Result(products));
	}

	public record Params(UUID sellerId) {
	}

	public record Result(List<ProductDTO> products) {
	}
}
