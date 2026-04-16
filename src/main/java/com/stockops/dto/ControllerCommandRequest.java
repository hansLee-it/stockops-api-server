package com.stockops.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Controller command bridge request payload.
 *
 * @param status requested controller status, on or off
 * @param outputLevel requested output level from 0 to 100
 * @author StockOps Team
 * @since 1.0
 */
public record ControllerCommandRequest(
        @NotBlank @Pattern(regexp = "on|off") String status,
        @NotNull @Min(0) @Max(100) Integer outputLevel) {
}
