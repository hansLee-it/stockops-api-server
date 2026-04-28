package com.stockops.service;

import com.stockops.dto.CreateProductRequest;
import com.stockops.dto.ProductDTO;
import com.stockops.dto.UpdateProductRequest;
import com.stockops.entity.Category;
import com.stockops.entity.Product;
import com.stockops.exception.ResourceNotFoundException;
import com.stockops.repository.CategoryRepository;
import com.stockops.repository.ProductRepository;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product master business logic.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Creates the product service.
     *
     * @param productRepository product repository
     * @param categoryRepository category repository
     */
    public ProductService(final ProductRepository productRepository,
                          final CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Creates a new product.
     * If categoryId is provided, validates that the category exists.
     *
     * @param request creation payload
     * @return created product
     */
    @Transactional
    public ProductDTO createProduct(final CreateProductRequest request) {
        if (productRepository.existsByBarcodeAndDeletedFalse(request.barcode())) {
            throw new IllegalArgumentException("Barcode already exists");
        }

        final Product product = new Product();
        product.setBarcode(request.barcode());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setCategory(request.category());
        product.setCategoryId(request.categoryId());
        product.setUnit(request.unit());
        product.setExpiryManaged(request.expiryManaged());
        product.setDefaultPrice(resolveDefaultPrice(request.defaultPrice()));
        product.setSafetyStockQuantity(resolveSafetyStockQuantity(request.safetyStockQuantity()));

        if (request.categoryId() != null) {
            validateCategoryExists(request.categoryId());
        }

        return toDTO(productRepository.save(product));
    }

    /**
     * Finds a product by id.
     *
     * @param id product id
     * @return product response
     */
    @Transactional(readOnly = true)
    public ProductDTO getProductById(final Long id) {
        return toDTO(findProductById(id));
    }

    /**
     * Finds a product by barcode.
     *
     * @param barcode product barcode
     * @return product response
     */
    @Transactional(readOnly = true)
    public ProductDTO getProductByBarcode(final String barcode) {
        return toDTO(productRepository.findByBarcodeAndDeletedFalse(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + barcode)));
    }

    /**
     * Returns products in a paged response.
     *
     * @param pageable paging parameters
     * @return paged products
     */
    @Transactional(readOnly = true)
    public Page<ProductDTO> getProducts(final Pageable pageable) {
        return productRepository.findAllByDeletedFalse(pageable).map(this::toDTO);
    }

    /**
     * Updates an existing product.
     * If categoryId is provided in the request, validates that the category exists.
     *
     * @param id product id
     * @param request update payload
     * @return updated product
     */
    @Transactional
    public ProductDTO updateProduct(final Long id, final UpdateProductRequest request) {
        final Product product = findProductById(id);

        if (request.name() != null) {
            product.setName(request.name());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.category() != null) {
            product.setCategory(request.category());
        }
        if (request.categoryId() != null) {
            validateCategoryExists(request.categoryId());
            product.setCategoryId(request.categoryId());
        }
        if (request.unit() != null) {
            product.setUnit(request.unit());
        }
        if (request.expiryManaged() != null) {
            product.setExpiryManaged(request.expiryManaged());
        }
        if (request.defaultPrice() != null) {
            product.setDefaultPrice(request.defaultPrice());
        }
        if (request.safetyStockQuantity() != null) {
            product.setSafetyStockQuantity(request.safetyStockQuantity());
        }

        return toDTO(productRepository.save(product));
    }

    /**
     * Soft deletes a product.
     * Preserves the row for history while removing it from active product lookups.
     *
     * @param id product id
     */
    @Transactional
    public void deleteProduct(final Long id) {
        final Product product = findProductById(id);
        product.setDeleted(true);
        productRepository.save(product);
    }

    private Product findProductById(final Long id) {
        return productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private void validateCategoryExists(final Long categoryId) {
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
    }

    private String resolveCategoryName(final Long categoryId) {
        if (categoryId == null) {
            return "";
        }
        return categoryRepository.findById(categoryId)
                .map(Category::getName)
                .orElse("");
    }

    private ProductDTO toDTO(final Product product) {
        return new ProductDTO(
                product.getId(),
                product.getBarcode(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getCategoryId(),
                resolveCategoryName(product.getCategoryId()),
                product.getUnit(),
                product.isExpiryManaged(),
                product.getDefaultPrice(),
                product.getSafetyStockQuantity(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }

    private BigDecimal resolveDefaultPrice(final BigDecimal defaultPrice) {
        return defaultPrice == null ? BigDecimal.ZERO : defaultPrice;
    }

    private Integer resolveSafetyStockQuantity(final Integer safetyStockQuantity) {
        return safetyStockQuantity == null ? 0 : safetyStockQuantity;
    }
}
