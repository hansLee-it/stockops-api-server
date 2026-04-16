package com.stockops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * Environment controller master entity.
 * Soft-deleted controllers remain preserved for command history while active queries exclude them.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Data
@Entity
@Table(name = "environment_controllers")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SQLRestriction("deleted = false")
public class EnvironmentController extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "external_controller_id", nullable = false)
    private String externalControllerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "controller_type", nullable = false, length = 50)
    private ControllerType controllerType = ControllerType.VENTILATION;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_axis", nullable = false, length = 50)
    private EnvironmentAxis targetAxis;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ControllerStatus status = ControllerStatus.INACTIVE;

    @Column(name = "output_level", nullable = false)
    private Integer outputLevel = 0;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
