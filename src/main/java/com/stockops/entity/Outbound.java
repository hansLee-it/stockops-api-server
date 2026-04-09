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
 * Outbound header entity.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Data
@Entity
@Table(name = "outbounds")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Outbound extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "outbound_date", nullable = false)
    private LocalDate outboundDate;

    @Column(name = "customer")
    private String customer;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "created_by")
    private Long createdBy;
}
