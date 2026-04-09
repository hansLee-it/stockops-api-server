package com.stockops.repository;

import com.stockops.entity.StockAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {
}
