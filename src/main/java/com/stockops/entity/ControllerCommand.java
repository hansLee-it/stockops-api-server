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

/**
 * Append-only controller command history entity.
 * Stores requested controller actions and downstream processing results.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Data
@Entity
@Table(name = "controller_commands")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ControllerCommand extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "controller_id", nullable = false)
    private Long controllerId;

    @Column(name = "requested_status", nullable = false, length = 30)
    private String requestedStatus = ControllerCommandResultStatus.PENDING.name();

    @Column(name = "requested_output_level")
    private Integer requestedOutputLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 30)
    private ControllerCommandResultStatus resultStatus = ControllerCommandResultStatus.PENDING;

    @Column(name = "result_message", columnDefinition = "TEXT")
    private String resultMessage;

    @Column(name = "sensimul_response_code", length = 100)
    private String sensimulResponseCode;
}
