package com.stockops.dto;

import com.stockops.entity.ControllerCommandResultStatus;
import java.time.Instant;

/**
 * Controller command history response payload.
 *
 * @param id command identifier
 * @param controllerId environment controller identifier
 * @param requestedStatus requested downstream status value
 * @param requestedOutputLevel requested downstream output level
 * @param resultStatus command bridge result status
 * @param resultMessage result description
 * @param sensimulResponseCode Sensimul response classification or HTTP code
 * @param createdAt command creation timestamp
 * @author StockOps Team
 * @since 1.0
 */
public record ControllerCommandResponse(
        Long id,
        Long controllerId,
        String requestedStatus,
        Integer requestedOutputLevel,
        ControllerCommandResultStatus resultStatus,
        String resultMessage,
        String sensimulResponseCode,
        Instant createdAt) {
}
