package com.stockops.service;

import com.stockops.entity.DemandForecast;
import com.stockops.entity.InventoryTransaction;
import com.stockops.repository.DemandForecastRepository;
import com.stockops.repository.InventoryTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DemandForecastService {

    private final DemandForecastRepository forecastRepository;
    private final InventoryTransactionRepository transactionRepository;

    public List<Map<String, Object>> generateForecast(Long productId, int daysAhead) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(30);
        LocalDate end = today.minusDays(1);

        List<InventoryTransaction> history = transactionRepository
            .findByProductIdAndTransactionDateBetween(productId, start, end);

        Map<LocalDate, BigDecimal> dailyNet = new HashMap<>();
        for (InventoryTransaction tx : history) {
            LocalDate d = tx.getTransactionDate();
            dailyNet.merge(d, tx.getQuantity().negate(), BigDecimal::add);
        }

        List<BigDecimal> values = new ArrayList<>(dailyNet.values());
        if (values.isEmpty()) {
            return Collections.emptyList();
        }

        double avg = values.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        double variance = values.stream().mapToDouble(v -> Math.pow(v.doubleValue() - avg, 2)).average().orElse(0);
        double stdDev = Math.sqrt(variance);

        List<Map<String, Object>> forecasts = new ArrayList<>();
        for (int i = 1; i <= daysAhead; i++) {
            LocalDate date = today.plusDays(i);
            double predicted = avg * (1 + 0.01 * i);
            double lower = predicted - 1.96 * stdDev;
            double upper = predicted + 1.96 * stdDev;

            DemandForecast f = new DemandForecast();
            f.setProductId(productId);
            f.setForecastDate(date);
            f.setPredictedQuantity(BigDecimal.valueOf(predicted).setScale(2, RoundingMode.HALF_UP));
            f.setConfidenceLower(BigDecimal.valueOf(Math.max(0, lower)).setScale(2, RoundingMode.HALF_UP));
            f.setConfidenceUpper(BigDecimal.valueOf(upper).setScale(2, RoundingMode.HALF_UP));
            f.setModelVersion("moving-avg-v1");

            forecastRepository.save(f);

            forecasts.add(Map.of(
                "date", date.toString(),
                "predicted", f.getPredictedQuantity(),
                "lower", f.getConfidenceLower(),
                "upper", f.getConfidenceUpper()
            ));
        }
        return forecasts;
    }

    public List<Map<String, Object>> getLowStockProducts() {
        return Collections.emptyList();
    }
}