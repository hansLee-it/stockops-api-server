package com.stockops.repository;

import com.stockops.entity.DemandForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface DemandForecastRepository extends JpaRepository<DemandForecast, Long> {
    List<DemandForecast> findByProductIdAndForecastDateBetweenOrderByForecastDateAsc(
        Long productId, LocalDate start, LocalDate end);

    @Query("SELECT df FROM DemandForecast df WHERE df.forecastDate = :date ORDER BY df.productId")
    List<DemandForecast> findByForecastDate(@Param("date") LocalDate date);
}