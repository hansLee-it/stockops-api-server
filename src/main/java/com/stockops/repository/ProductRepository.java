package com.stockops.repository;

import com.stockops.entity.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndDeletedFalse(Long id);

    Optional<Product> findByBarcodeAndDeletedFalse(String barcode);

    boolean existsByBarcodeAndDeletedFalse(String barcode);

    Page<Product> findAllByDeletedFalse(Pageable pageable);
}
