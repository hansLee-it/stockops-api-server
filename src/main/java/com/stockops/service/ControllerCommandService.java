package com.stockops.service;

import com.stockops.dto.ControllerCommandRequest;
import com.stockops.dto.ControllerCommandResponse;
import com.stockops.entity.ControllerCommand;
import com.stockops.entity.ControllerCommandResultStatus;
import com.stockops.entity.EnvironmentController;
import com.stockops.exception.InvalidOperationException;
import com.stockops.exception.ResourceNotFoundException;
import com.stockops.integration.sensimul.ControllerUpdateRequest;
import com.stockops.integration.sensimul.ParsedControllerTopic;
import com.stockops.integration.sensimul.SensimulControllerClient;
import com.stockops.integration.sensimul.SensimulIntegrationException;
import com.stockops.integration.sensimul.SensimulTopics;
import com.stockops.repository.ControllerCommandRepository;
import com.stockops.repository.EnvironmentControllerRepository;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bridges controller command API requests to Sensimul and persists audit history.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Service
public class ControllerCommandService {

    private final EnvironmentControllerRepository environmentControllerRepository;

    private final ControllerCommandRepository controllerCommandRepository;

    private final SensimulControllerClient sensimulControllerClient;

    /**
     * Creates the service.
     *
     * @param environmentControllerRepository environment controller repository
     * @param controllerCommandRepository controller command repository
     * @param sensimulControllerClient Sensimul controller HTTP client
     */
    public ControllerCommandService(
            final EnvironmentControllerRepository environmentControllerRepository,
            final ControllerCommandRepository controllerCommandRepository,
            final SensimulControllerClient sensimulControllerClient) {
        this.environmentControllerRepository = environmentControllerRepository;
        this.controllerCommandRepository = controllerCommandRepository;
        this.sensimulControllerClient = sensimulControllerClient;
    }

    /**
     * Forwards a controller command to Sensimul and stores the audit record.
     *
     * @param controllerId environment controller identifier
     * @param request command request
     * @return persisted command response
     */
    @Transactional
    public ControllerCommandResponse sendCommand(final Long controllerId, final ControllerCommandRequest request) {
        final EnvironmentController controller = findActiveController(controllerId);
        final ParsedControllerTopic parsedTopic = parseTopic(controller);

        final ControllerCommand command = new ControllerCommand();
        command.setControllerId(controller.getId());
        command.setRequestedStatus(request.status());
        command.setRequestedOutputLevel(request.outputLevel());
        command.setResultStatus(ControllerCommandResultStatus.FORWARDED);
        command.setResultMessage("Command accepted for forwarding to Sensimul");
        command.setSensimulResponseCode("FORWARDED");
        controllerCommandRepository.save(command);

        try {
            sensimulControllerClient.updateController(parsedTopic.siteId(), parsedTopic.controllerId(),
                    new ControllerUpdateRequest(request.status(), request.outputLevel()));
            command.setResultStatus(ControllerCommandResultStatus.APPLIED);
            command.setResultMessage("Command applied by Sensimul");
            command.setSensimulResponseCode("2xx/303");
            return toResponse(controllerCommandRepository.save(command));
        } catch (InvalidOperationException ex) {
            command.setResultStatus(ControllerCommandResultStatus.FAILED_RETRYABLE);
            command.setResultMessage(ex.getMessage());
            command.setSensimulResponseCode("4xx");
            controllerCommandRepository.save(command);
            throw ex;
        } catch (SensimulIntegrationException ex) {
            command.setResultStatus(ControllerCommandResultStatus.FAILED_RETRYABLE);
            command.setResultMessage(ex.getMessage());
            final HttpStatus status = resolveIntegrationStatus(ex);
            command.setSensimulResponseCode(String.valueOf(status.value()));
            controllerCommandRepository.save(command);
            throw ex;
        }
    }

    /**
     * Returns recent command history for an active controller.
     *
     * @param controllerId environment controller identifier
     * @param pageable paging parameters
     * @return command history sorted newest first
     */
    @Transactional(readOnly = true)
    public List<ControllerCommandResponse> getCommandHistory(final Long controllerId, final Pageable pageable) {
        findActiveController(controllerId);
        return controllerCommandRepository.findByControllerIdOrderByCreatedAtDesc(controllerId, pageable).stream()
                .map(this::toResponse)
                .toList();
    }

    private EnvironmentController findActiveController(final Long controllerId) {
        return environmentControllerRepository.findByIdAndDeletedFalse(controllerId)
                .orElseThrow(() -> new ResourceNotFoundException("Environment controller not found: " + controllerId));
    }

    private ParsedControllerTopic parseTopic(final EnvironmentController controller) {
        final String topic = controller.getMqttTopic() != null
                ? controller.getMqttTopic()
                : controller.getExternalControllerId();
        return SensimulTopics.parseLiveControllerTopic(topic)
                .orElseThrow(() -> new IllegalStateException(
                        "Environment controller topic is invalid: " + topic));
    }

    private HttpStatus resolveIntegrationStatus(final SensimulIntegrationException ex) {
        final String message = ex.getMessage() == null ? "" : ex.getMessage();
        return message.startsWith("Failed to ") ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY;
    }

    private ControllerCommandResponse toResponse(final ControllerCommand command) {
        return new ControllerCommandResponse(
                command.getId(),
                command.getControllerId(),
                command.getRequestedStatus(),
                command.getRequestedOutputLevel(),
                command.getResultStatus(),
                command.getResultMessage(),
                command.getSensimulResponseCode(),
                command.getCreatedAt());
    }
}
