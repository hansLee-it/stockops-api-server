package com.stockops.repository;

import com.stockops.entity.OutboundItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboundItemRepository extends JpaRepository<OutboundItem, Long> {

    List<OutboundItem> findByOutboundId(Long outboundId);
}
