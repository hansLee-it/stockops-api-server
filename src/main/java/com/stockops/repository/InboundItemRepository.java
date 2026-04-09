package com.stockops.repository;

import com.stockops.entity.InboundItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboundItemRepository extends JpaRepository<InboundItem, Long> {
}
