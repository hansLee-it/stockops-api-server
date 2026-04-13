package com.stockops.service;

import com.stockops.dto.CreateProductRequest;
import com.stockops.dto.ProductDTO;
import com.stockops.dto.UpdateProductRequest;
import com.stockops.entity.Product;
import com.stockops.exception.ResourceNotFoundException;
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

    /**
     * Creates the product service.
     *
     * @param productRepository product repository
     */
    public ProductService(final ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Creates a new product.
     *
     * @param request creation payload
     * @return created product
     */
    @Transactional
    public ProductDTO createProduct(final CreateProductRequest request) {
        if (productRepository.existsByBarcode(request.barcode())) {
            throw new IllegalArgumentException("Barcode already exists");
        }

        final Product product = new Product();
        product.setBarcode(request.barcode());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setCategory(request.category());
        product.setUnit(request.unit());
        product.setExpiryManaged(request.expiryManaged());
        product.setDefaultPrice(resolveDefaultPrice(request.defaultPrice()));
        product.setSafetyStockQuantity(resolveSafetyStockQuantity(request.safetyStockQuantity()));

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
        return toDTO(productRepository.findByBarcode(barcode)
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
        return productRepository.findAll(pageable).map(this::toDTO);
    }

    /**
     * Updates an existing product.
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
     * Deletes a product.
     *
     * @param id product id
     */
    @Transactional
    public void deleteProduct(final Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found: " + id);
        }

        productRepository.deleteById(id);
    }

    private Product findProductById(final Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private ProductDTO toDTO(final Product product) {
        return new ProductDTO(
                product.getId(),
                product.getBarcode(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
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
