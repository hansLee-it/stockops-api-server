package com.stockops.repository;

import com.stockops.entity.Outbound;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboundRepository extends JpaRepository<Outbound, Long> {

    List<Outbound> findByStatus(String status);
}
