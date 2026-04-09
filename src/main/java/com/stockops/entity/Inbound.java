package com.stockops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Inbound header entity.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Data
@Entity
@Table(name = "inbounds")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Inbound extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inbound_date", nullable = false)
    private LocalDate inboundDate;

    @Column(name = "supplier")
    private String supplier;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "created_by")
    private Long createdBy;
}
