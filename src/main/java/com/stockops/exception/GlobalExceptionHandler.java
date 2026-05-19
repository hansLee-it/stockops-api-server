package com.stockops.exception;

import com.stockops.integration.sensimul.SensimulIntegrationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global API exception mapping.
 *
 * @author StockOps Team
 * @since 1.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles missing resources.
     *
     * @param ex resource not found exception
     * @return 404 error response
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(final ResourceNotFoundException ex) {
        return ResponseEntity.status(404).body(new ErrorResponse(404, ex.getMessage()));
    }

    /**
     * Handles resource state conflicts.
     *
     * @param ex conflict exception
     * @return 409 error response
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(final ConflictException ex) {
        return ResponseEntity.status(409).body(new ErrorResponse(409, ex.getMessage()));
    }

    /**
     * Handles scoped authorization failures.
     *
     * @param ex forbidden exception
     * @return 403 error response
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(final ForbiddenException ex) {
        return ResponseEntity.status(403).body(new ErrorResponse(403, ex.getMessage()));
    }

    /**
     * Handles stock conflicts caused by insufficient inventory.
     *
     * @param ex insufficient stock exception
     * @return 409 error response
     */
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(final InsufficientStockException ex) {
        return ResponseEntity.status(409).body(new ErrorResponse(409, ex.getMessage()));
    }

    /**
     * Handles invalid inventory operations.
     *
     * @param ex invalid operation exception
     * @return 400 error response
     */
    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOperation(final InvalidOperationException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(400, ex.getMessage()));
    }

    /**
     * Handles bean validation failures.
     *
     * @param ex validation exception
     * @return 400 error response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(final MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(400, "Validation failed"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(final IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(400, ex.getMessage()));
    }

    /**
     * Handles downstream Sensimul integration failures.
     *
     * @param ex Sensimul integration exception
     * @return 502/503 error response based on integration failure type
     */
    @ExceptionHandler(SensimulIntegrationException.class)
    public ResponseEntity<ErrorResponse> handleSensimulIntegration(final SensimulIntegrationException ex) {
        final HttpStatus status = resolveIntegrationStatus(ex);
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), ex.getMessage()));
    }

    private HttpStatus resolveIntegrationStatus(final SensimulIntegrationException ex) {
        final String message = ex.getMessage() == null ? "" : ex.getMessage();
        return message.startsWith("Failed to ") ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY;
    }
}
