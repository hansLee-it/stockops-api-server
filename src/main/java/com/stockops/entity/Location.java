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

/**
 * Location master entity.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Data
@Entity
@Table(name = "locations")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Location extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "zone")
    private String zone;

    @Column(name = "shelf")
    private String shelf;

    @Column(name = "level")
    private String level;
}
