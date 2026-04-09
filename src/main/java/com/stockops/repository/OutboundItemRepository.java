package com.stockops.repository;

import com.stockops.entity.OutboundItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboundItemRepository extends JpaRepository<OutboundItem, Long> {
}
