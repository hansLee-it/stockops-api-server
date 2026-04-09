package com.stockops.repository;

import com.stockops.entity.ReasonCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReasonCodeRepository extends JpaRepository<ReasonCode, Long> {

    Optional<ReasonCode> findByCode(String code);

    boolean existsByCode(String code);

    List<ReasonCode> findByCategory(String category);
}
