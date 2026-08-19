package com.caioms.java_marketplace.modules.catalog.application.use_cases;

import com.caioms.java_marketplace.modules.catalog.application.dto.ProductDTO;
import com.caioms.java_marketplace.modules.catalog.application.repositories.ProductRepository;
import io.vavr.control.Either;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListPublicProductsUseCase {
	private final ProductRepository productRepository;

	Either<Void, Result> execute(Params params) {
		var pageable = PageRequest.of(params.page(), params.size());
		var page = productRepository.search(params.categoryId(), params.minPrice(),
		        params.maxPrice(), params.search(), pageable);

		var products = page.getContent().stream()
		        .map(product -> new ProductDTO(product.getId(), product.getName(),
		                product.getPrice(), product.getDescription(), product.getCategories()))
		        .toList();

		return Either.right(new Result(products, page.getTotalElements(), page.getTotalPages()));
	}

	public record Result(List<ProductDTO> products, long totalElements, int totalPages) {
	}

	public record Params(UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice, String search,
	        int page, int size) {
		public Params {
			if (page < 0)
				page = 0;
			if (size <= 0)
				size = 20;
		}
	}

}
