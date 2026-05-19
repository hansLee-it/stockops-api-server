package com.stockops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Append-only controller command history entity.
 * Stores requested controller actions and downstream processing results.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Entity
@Table(name = "controller_commands")
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

    public ControllerCommand() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getControllerId() {
        return this.controllerId;
    }

    public void setControllerId(final Long controllerId) {
        this.controllerId = controllerId;
    }

    public String getRequestedStatus() {
        return this.requestedStatus;
    }

    public void setRequestedStatus(final String requestedStatus) {
        this.requestedStatus = requestedStatus;
    }

    public Integer getRequestedOutputLevel() {
        return this.requestedOutputLevel;
    }

    public void setRequestedOutputLevel(final Integer requestedOutputLevel) {
        this.requestedOutputLevel = requestedOutputLevel;
    }

    public ControllerCommandResultStatus getResultStatus() {
        return this.resultStatus;
    }

    public void setResultStatus(final ControllerCommandResultStatus resultStatus) {
        this.resultStatus = resultStatus;
    }

    public String getResultMessage() {
        return this.resultMessage;
    }

    public void setResultMessage(final String resultMessage) {
        this.resultMessage = resultMessage;
    }

    public String getSensimulResponseCode() {
        return this.sensimulResponseCode;
    }

    public void setSensimulResponseCode(final String sensimulResponseCode) {
        this.sensimulResponseCode = sensimulResponseCode;
    }
}
