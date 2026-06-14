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
import com.stockops.environment.command.CommandMessagingProperties;
import com.stockops.environment.command.ControllerCommandMessage;
import com.stockops.environment.command.ControllerCommandPublisher;
import com.stockops.repository.ControllerCommandRepository;
import com.stockops.repository.EnvironmentControllerRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    private final CommandMessagingProperties commandMessagingProperties;

    private final Optional<ControllerCommandPublisher> commandPublisher;

    /**
     * Creates the service.
     *
     * @param environmentControllerRepository environment controller repository
     * @param controllerCommandRepository controller command repository
     * @param sensimulControllerClient Sensimul controller HTTP client (legacy path)
     * @param commandMessagingProperties command-messaging configuration
     * @param commandPublisher MQTT command publisher (present only when messaging is enabled)
     */
    public ControllerCommandService(
            final EnvironmentControllerRepository environmentControllerRepository,
            final ControllerCommandRepository controllerCommandRepository,
            final SensimulControllerClient sensimulControllerClient,
            final CommandMessagingProperties commandMessagingProperties,
            final Optional<ControllerCommandPublisher> commandPublisher) {
        this.environmentControllerRepository = environmentControllerRepository;
        this.controllerCommandRepository = controllerCommandRepository;
        this.sensimulControllerClient = sensimulControllerClient;
        this.commandMessagingProperties = commandMessagingProperties;
        this.commandPublisher = commandPublisher;
    }

    /**
     * Sends a controller command and stores the audit record. When command messaging is enabled
     * the command is published to the broker (PENDING → SENT) and finalized asynchronously by the
     * ACK subscriber; otherwise it forwards synchronously over HTTP (PENDING → FORWARDED → APPLIED).
     *
     * @param controllerId environment controller identifier
     * @param request command request
     * @return persisted command response
     */
    @Transactional
    public ControllerCommandResponse sendCommand(final Long controllerId, final ControllerCommandRequest request) {
        final EnvironmentController controller = findActiveController(controllerId);
        final ParsedControllerTopic parsedTopic = parseTopic(controller);

        if (commandMessagingProperties.isEnabled() && commandPublisher.isPresent()) {
            return sendViaMessaging(controller, parsedTopic, request);
        }

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
     * Publishes the command to the broker, persisting PENDING → SENT. The ACK subscriber later
     * transitions it to ACKED/FAILED, and the scheduled sweep to TIMEOUT if no ACK arrives.
     */
    private ControllerCommandResponse sendViaMessaging(final EnvironmentController controller,
                                                       final ParsedControllerTopic parsedTopic,
                                                       final ControllerCommandRequest request) {
        final String correlationId = UUID.randomUUID().toString();
        final ControllerCommand command = new ControllerCommand();
        command.setControllerId(controller.getId());
        command.setRequestedStatus(request.status());
        command.setRequestedOutputLevel(request.outputLevel());
        command.setCorrelationId(correlationId);
        command.setResultStatus(ControllerCommandResultStatus.PENDING);
        command.setResultMessage("명령 발행 대기");
        controllerCommandRepository.save(command);

        try {
            commandPublisher.get().publish(new ControllerCommandMessage(
                    correlationId, parsedTopic.siteId(), parsedTopic.controllerId(),
                    request.status(), request.outputLevel()));
            command.setResultStatus(ControllerCommandResultStatus.SENT);
            command.setResultMessage("브로커로 명령을 발행했습니다. ACK 대기 중");
        } catch (final RuntimeException ex) {
            command.setResultStatus(ControllerCommandResultStatus.FAILED);
            command.setResultMessage("명령 발행 실패: " + ex.getMessage());
        }
        return toResponse(controllerCommandRepository.save(command));
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
