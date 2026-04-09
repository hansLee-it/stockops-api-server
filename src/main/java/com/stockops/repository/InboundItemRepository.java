package com.stockops.repository;

import com.stockops.entity.InboundItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboundItemRepository extends JpaRepository<InboundItem, Long> {

    List<InboundItem> findByInboundId(Long inboundId);
}
