package com.stockops.service;

import com.stockops.ai.forecast.ForecastContext;
import com.stockops.ai.forecast.ForecastContext.DemandDataPoint;
import com.stockops.ai.forecast.ForecastContext.ForecastParameters;
import com.stockops.ai.forecast.ForecastContext.LeadTimeInfo;
import com.stockops.ai.forecast.ForecastModel;
import com.stockops.ai.forecast.ForecastResult;
import com.stockops.entity.DemandForecast;
import com.stockops.entity.InventoryTransaction;
import com.stockops.repository.DemandForecastRepository;
import com.stockops.repository.InventoryTransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class DemandForecastService {

    private final DemandForecastRepository forecastRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final ForecastModel statisticalForecastModel;

    public DemandForecastService(
            final DemandForecastRepository forecastRepository,
            final InventoryTransactionRepository transactionRepository,
            @Qualifier("statisticalForecastModel") final ForecastModel statisticalForecastModel) {
        this.forecastRepository = forecastRepository;
        this.transactionRepository = transactionRepository;
        this.statisticalForecastModel = statisticalForecastModel;
    }

    public List<Map<String, Object>> generateForecast(Long productId, int daysAhead) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(30);
        LocalDate end = today.minusDays(1);

        List<InventoryTransaction> history = transactionRepository
            .findByProductIdAndCreatedAtBetween(productId, start.atStartOfDay().toInstant(ZoneOffset.UTC), end.atTime(23, 59, 59).toInstant(ZoneOffset.UTC));

        Map<LocalDate, BigDecimal> dailyNet = new HashMap<>();
        for (InventoryTransaction tx : history) {
            LocalDate d = LocalDate.ofInstant(tx.getCreatedAt(), ZoneOffset.UTC);
            int qty = tx.getQuantity();
            dailyNet.merge(d, BigDecimal.valueOf(-qty), BigDecimal::add);
        }

        List<BigDecimal> values = new ArrayList<>(dailyNet.values());
        if (values.isEmpty()) {
            return Collections.emptyList();
        }

        List<DemandDataPoint> demandDataPoints = dailyNet.entrySet().stream()
                .map(entry -> new DemandDataPoint(entry.getKey(), entry.getValue().intValue(), 1))
                .sorted(Comparator.comparing(DemandDataPoint::businessDate))
                .toList();

        ForecastParameters parameters = new ForecastParameters(
                7, 4, daysAhead, 30,
                new BigDecimal("0.70"), new BigDecimal("0.30"));

        ForecastContext context = new ForecastContext(
                productId, null, null, today,
                0, 0,
                demandDataPoints,
                LeadTimeInfo.defaultFor(1),
                parameters);

        ForecastResult result = statisticalForecastModel.computeForecast(context);

        double avg = values.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        double variance = values.stream().mapToDouble(v -> Math.pow(v.doubleValue() - avg, 2)).average().orElse(0);
        double stdDev = Math.sqrt(variance);

        List<Map<String, Object>> forecasts = new ArrayList<>();
        for (int i = 1; i <= daysAhead; i++) {
            LocalDate date = today.plusDays(i);
            double predicted = result.weightedDailyDemand().doubleValue();
            double lower = predicted - 1.96 * stdDev;
            double upper = predicted + 1.96 * stdDev;

            DemandForecast f = new DemandForecast();
            f.setProductId(productId);
            f.setForecastDate(date);
            f.setPredictedQuantity(BigDecimal.valueOf(predicted).setScale(2, RoundingMode.HALF_UP));
            f.setConfidenceLower(BigDecimal.valueOf(Math.max(0, lower)).setScale(2, RoundingMode.HALF_UP));
            f.setConfidenceUpper(BigDecimal.valueOf(upper).setScale(2, RoundingMode.HALF_UP));
            f.setModelVersion(result.modelVersion());

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
