package com.stockops.controller;

import com.stockops.dto.CreateProductRequest;
import com.stockops.dto.ProductDTO;
import com.stockops.dto.UpdateProductRequest;
import com.stockops.service.ProductService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Product master API controller.
 *
 * @author StockOps Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    /**
     * Creates the controller.
     *
     * @param productService product service
     */
    public ProductController(final ProductService productService) {
        this.productService = productService;
    }

    /**
     * Creates a product.
     * Supports optional default pricing and safety stock targets for downstream planning.
     *
     * @param request creation payload
     * @return created product
     */
    @PostMapping
    @PreAuthorize("@permissionChecker.hasPermission('PRODUCT_CREATE')")
    public ResponseEntity<ProductDTO> createProduct(@Valid @RequestBody final CreateProductRequest request) {
        final ProductDTO product = productService.createProduct(request);
        return ResponseEntity.created(URI.create("/api/v1/products/" + product.id())).body(product);
    }

    /**
     * Returns a product by id.
     *
     * @param id product id
     * @return product response
     */
    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('PRODUCT_READ')")
    public ResponseEntity<ProductDTO> getProduct(@PathVariable final Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    /**
     * Returns a product by barcode.
     *
     * @param barcode product barcode
     * @return product response
     */
    @GetMapping("/barcode/{barcode}")
    @PreAuthorize("@permissionChecker.hasPermission('PRODUCT_READ')")
    public ResponseEntity<ProductDTO> getProductByBarcode(@PathVariable final String barcode) {
        return ResponseEntity.ok(productService.getProductByBarcode(barcode));
    }

    /**
     * Returns paged products.
     *
     * @param pageable paging parameters
     * @return paged product response
     */
    @GetMapping
    @PreAuthorize("@permissionChecker.hasPermission('PRODUCT_READ')")
    public ResponseEntity<Page<ProductDTO>> getAllProducts(
            @PageableDefault(size = 20) final Pageable pageable) {
        return ResponseEntity.ok(productService.getProducts(pageable));
    }

    /**
     * Updates a product.
     * Accepts additive inventory planning fields without changing the existing endpoint contract.
     *
     * @param id product id
     * @param request update payload
     * @return updated product
     */
    @PutMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('PRODUCT_UPDATE')")
    public ResponseEntity<ProductDTO> updateProduct(
            @PathVariable final Long id,
            @Valid @RequestBody final UpdateProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    /**
     * Soft deletes a product.
     * The product remains stored for history but disappears from active product APIs.
     *
     * @param id product id
     * @return no content response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('PRODUCT_DELETE')")
    public ResponseEntity<Void> deleteProduct(@PathVariable final Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
