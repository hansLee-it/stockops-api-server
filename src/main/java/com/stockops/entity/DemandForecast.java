package com.stockops.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "demand_forecasts")
@SQLRestriction("deleted = false")
public class DemandForecast extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "forecast_date", nullable = false)
    private LocalDate forecastDate;

    @Column(name = "predicted_quantity", nullable = false)
    private BigDecimal predictedQuantity;

    @Column(name = "confidence_lower")
    private BigDecimal confidenceLower;

    @Column(name = "confidence_upper")
    private BigDecimal confidenceUpper;

    @Column(name = "model_version")
    private String modelVersion = "v1.0";

    public DemandForecast() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return this.productId;
    }

    public void setProductId(final Long productId) {
        this.productId = productId;
    }

    public LocalDate getForecastDate() {
        return this.forecastDate;
    }

    public void setForecastDate(final LocalDate forecastDate) {
        this.forecastDate = forecastDate;
    }

    public BigDecimal getPredictedQuantity() {
        return this.predictedQuantity;
    }

    public void setPredictedQuantity(final BigDecimal predictedQuantity) {
        this.predictedQuantity = predictedQuantity;
    }

    public BigDecimal getConfidenceLower() {
        return this.confidenceLower;
    }

    public void setConfidenceLower(final BigDecimal confidenceLower) {
        this.confidenceLower = confidenceLower;
    }

    public BigDecimal getConfidenceUpper() {
        return this.confidenceUpper;
    }

    public void setConfidenceUpper(final BigDecimal confidenceUpper) {
        this.confidenceUpper = confidenceUpper;
    }

    public String getModelVersion() {
        return this.modelVersion;
    }

    public void setModelVersion(final String modelVersion) {
        this.modelVersion = modelVersion;
    }
}