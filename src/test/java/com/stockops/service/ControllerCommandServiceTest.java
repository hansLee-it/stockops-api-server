package com.stockops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockops.dto.ControllerCommandRequest;
import com.stockops.dto.ControllerCommandResponse;
import com.stockops.entity.ControllerCommand;
import com.stockops.entity.ControllerCommandResultStatus;
import com.stockops.entity.ControllerStatus;
import com.stockops.entity.ControllerType;
import com.stockops.entity.EnvironmentAxis;
import com.stockops.entity.EnvironmentController;
import com.stockops.exception.InvalidOperationException;
import com.stockops.exception.ResourceNotFoundException;
import com.stockops.integration.sensimul.ControllerUpdateRequest;
import com.stockops.integration.sensimul.SensimulControllerClient;
import com.stockops.integration.sensimul.SensimulIntegrationException;
import com.stockops.repository.ControllerCommandRepository;
import com.stockops.repository.EnvironmentControllerRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

/**
 * Unit tests for {@link ControllerCommandService}.
 *
 * @author StockOps Team
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class ControllerCommandServiceTest {

    @Mock
    private EnvironmentControllerRepository environmentControllerRepository;

    @Mock
    private ControllerCommandRepository controllerCommandRepository;

    @Mock
    private SensimulControllerClient sensimulControllerClient;

    @InjectMocks
    private ControllerCommandService controllerCommandService;

    /**
     * Verifies that successful forwarding persists both the accepted and applied command states.
     */
    @Test
    void sendCommandAppliesControllerUpdateAndReturnsAppliedResponse() {
        final EnvironmentController controller = activeController(10L, "sensimul/sites/site-a/controllers/controller-01");
        final ControllerCommandRequest request = new ControllerCommandRequest("on", 80);
        final List<ControllerCommand> savedCommands = new ArrayList<>();
        when(environmentControllerRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(controller));
        when(controllerCommandRepository.save(any(ControllerCommand.class))).thenAnswer(invocation -> {
            final ControllerCommand saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(100L);
                saved.setCreatedAt(Instant.parse("2026-04-10T00:00:00Z"));
            }
            savedCommands.add(copyCommand(saved));
            return saved;
        });

        final ControllerCommandResponse response = controllerCommandService.sendCommand(10L, request);

        verify(sensimulControllerClient).updateController(eq("site-a"), eq("controller-01"),
                eq(new ControllerUpdateRequest("on", 80)));
        verify(controllerCommandRepository, times(2)).save(any(ControllerCommand.class));
        assertThat(savedCommands).extracting(ControllerCommand::getResultStatus)
                .containsExactly(ControllerCommandResultStatus.FORWARDED, ControllerCommandResultStatus.APPLIED);
        assertThat(response.resultStatus()).isEqualTo(ControllerCommandResultStatus.APPLIED);
        assertThat(response.sensimulResponseCode()).isEqualTo("2xx/303");
    }

    /**
     * Verifies that client-side validation failures are persisted as retryable 4xx bridge failures.
     */
    @Test
    void sendCommandPersistsFailureForInvalidOperationException() {
        final EnvironmentController controller = activeController(10L, "sensimul/sites/site-a/controllers/controller-01");
        final ControllerCommandRequest request = new ControllerCommandRequest("off", 0);
        final List<ControllerCommand> savedCommands = new ArrayList<>();
        when(environmentControllerRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(controller));
        when(controllerCommandRepository.save(any(ControllerCommand.class))).thenAnswer(invocation -> {
            final ControllerCommand saved = invocation.getArgument(0);
            savedCommands.add(copyCommand(saved));
            return saved;
        });
        final InvalidOperationException exception = new InvalidOperationException("status must be 'on' or 'off'");
        org.mockito.Mockito.doThrow(exception).when(sensimulControllerClient)
                .updateController(eq("site-a"), eq("controller-01"), any(ControllerUpdateRequest.class));

        final InvalidOperationException thrown = assertThrows(InvalidOperationException.class,
                () -> controllerCommandService.sendCommand(10L, request));

        assertThat(thrown).hasMessage("status must be 'on' or 'off'");
        verify(controllerCommandRepository, times(2)).save(any(ControllerCommand.class));
        assertThat(savedCommands.get(1).getResultStatus()).isEqualTo(ControllerCommandResultStatus.FAILED_RETRYABLE);
        assertThat(savedCommands.get(1).getSensimulResponseCode()).isEqualTo("4xx");
    }

    /**
     * Verifies that transport failures map to a 503 retryable bridge error code.
     */
    @Test
    void sendCommandPersistsServiceUnavailableForTransportFailure() {
        final EnvironmentController controller = activeController(10L, "sensimul/sites/site-a/controllers/controller-01");
        final ControllerCommandRequest request = new ControllerCommandRequest("on", 10);
        final List<ControllerCommand> savedCommands = new ArrayList<>();
        when(environmentControllerRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(controller));
        when(controllerCommandRepository.save(any(ControllerCommand.class))).thenAnswer(invocation -> {
            final ControllerCommand saved = invocation.getArgument(0);
            savedCommands.add(copyCommand(saved));
            return saved;
        });
        org.mockito.Mockito.doThrow(new SensimulIntegrationException("Failed to update controller via Sensimul"))
                .when(sensimulControllerClient)
                .updateController(eq("site-a"), eq("controller-01"), any(ControllerUpdateRequest.class));

        final SensimulIntegrationException thrown = assertThrows(SensimulIntegrationException.class,
                () -> controllerCommandService.sendCommand(10L, request));

        assertThat(thrown).hasMessage("Failed to update controller via Sensimul");
        verify(controllerCommandRepository, times(2)).save(any(ControllerCommand.class));
        assertThat(savedCommands.get(1).getSensimulResponseCode()).isEqualTo("503");
    }

    /**
     * Verifies that downstream 5xx-like integration failures map to a 502 bridge error code.
     */
    @Test
    void sendCommandPersistsBadGatewayForNonTransportIntegrationFailure() {
        final EnvironmentController controller = activeController(10L, "sensimul/sites/site-a/controllers/controller-01");
        final ControllerCommandRequest request = new ControllerCommandRequest("on", 10);
        final List<ControllerCommand> savedCommands = new ArrayList<>();
        when(environmentControllerRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(controller));
        when(controllerCommandRepository.save(any(ControllerCommand.class))).thenAnswer(invocation -> {
            final ControllerCommand saved = invocation.getArgument(0);
            savedCommands.add(copyCommand(saved));
            return saved;
        });
        org.mockito.Mockito.doThrow(new SensimulIntegrationException("Sensimul returned HTTP 500"))
                .when(sensimulControllerClient)
                .updateController(eq("site-a"), eq("controller-01"), any(ControllerUpdateRequest.class));

        assertThrows(SensimulIntegrationException.class, () -> controllerCommandService.sendCommand(10L, request));

        verify(controllerCommandRepository, times(2)).save(any(ControllerCommand.class));
        assertThat(savedCommands.get(1).getSensimulResponseCode()).isEqualTo("502");
    }

    /**
     * Verifies that invalid stored controller topics fail fast before outbound calls.
     */
    @Test
    void sendCommandRejectsInvalidControllerTopic() {
        when(environmentControllerRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(activeController(10L, "invalid-topic")));

        assertThrows(IllegalStateException.class,
                () -> controllerCommandService.sendCommand(10L, new ControllerCommandRequest("on", 50)));
    }

    /**
     * Verifies that command requests reject missing active controllers.
     */
    @Test
    void sendCommandRejectsMissingActiveController() {
        when(environmentControllerRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> controllerCommandService.sendCommand(10L, new ControllerCommandRequest("on", 50)));
    }

    /**
     * Verifies that command history returns newest-first mapped responses for an active controller.
     */
    @Test
    void getCommandHistoryReturnsMappedResponses() {
        final ControllerCommand command = new ControllerCommand();
        command.setId(100L);
        command.setControllerId(10L);
        command.setRequestedStatus("off");
        command.setRequestedOutputLevel(15);
        command.setResultStatus(ControllerCommandResultStatus.APPLIED);
        command.setResultMessage("done");
        command.setSensimulResponseCode("2xx/303");
        command.setCreatedAt(Instant.parse("2026-04-10T00:00:00Z"));
        when(environmentControllerRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(activeController(10L, "sensimul/sites/site-a/controllers/controller-01")));
        when(controllerCommandRepository.findByControllerIdOrderByCreatedAtDesc(10L, PageRequest.of(0, 5)))
                .thenReturn(List.of(command));

        final List<ControllerCommandResponse> history = controllerCommandService.getCommandHistory(10L, PageRequest.of(0, 5));

        assertThat(history).singleElement().satisfies(response -> {
            assertThat(response.id()).isEqualTo(100L);
            assertThat(response.requestedStatus()).isEqualTo("off");
            assertThat(response.resultStatus()).isEqualTo(ControllerCommandResultStatus.APPLIED);
        });
    }

    /**
     * Verifies that history queries return an empty list when a controller has no commands yet.
     */
    @Test
    void getCommandHistoryReturnsEmptyListWhenNoCommandsExist() {
        when(environmentControllerRepository.findByIdAndDeletedFalse(10L))
                .thenReturn(Optional.of(activeController(10L, "sensimul/sites/site-a/controllers/controller-01")));
        when(controllerCommandRepository.findByControllerIdOrderByCreatedAtDesc(10L, PageRequest.of(0, 5)))
                .thenReturn(List.of());

        assertThat(controllerCommandService.getCommandHistory(10L, PageRequest.of(0, 5))).isEmpty();
    }

    /**
     * Verifies that invalid local command payloads fail before the downstream client is called.
     */
    @Test
    void sendCommandRejectsInvalidLocalPayloadBeforeCallingClient() {
        final EnvironmentController controller = activeController(10L, "sensimul/sites/site-a/controllers/controller-01");
        final List<ControllerCommand> savedCommands = new ArrayList<>();
        when(environmentControllerRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(controller));
        when(controllerCommandRepository.save(any(ControllerCommand.class))).thenAnswer(invocation -> {
            final ControllerCommand saved = invocation.getArgument(0);
            savedCommands.add(copyCommand(saved));
            return saved;
        });

        final InvalidOperationException thrown = assertThrows(InvalidOperationException.class,
                () -> controllerCommandService.sendCommand(10L, new ControllerCommandRequest("invalid", 50)));

        assertThat(thrown).hasMessage("status must be 'on' or 'off'");
        verify(sensimulControllerClient, org.mockito.Mockito.never())
                .updateController(any(), any(), any(ControllerUpdateRequest.class));
        assertThat(savedCommands).hasSize(2);
        assertThat(savedCommands.get(1).getResultStatus()).isEqualTo(ControllerCommandResultStatus.FAILED_RETRYABLE);
    }

    private EnvironmentController activeController(final Long id, final String topic) {
        final EnvironmentController controller = new EnvironmentController();
        controller.setId(id);
        controller.setName("controller-name");
        controller.setExternalControllerId(topic);
        controller.setControllerType(ControllerType.VENTILATION);
        controller.setTargetAxis(EnvironmentAxis.AIR_QUALITY);
        controller.setStatus(ControllerStatus.READY);
        controller.setOutputLevel(40);
        controller.setActive(true);
        controller.setDeleted(false);
        return controller;
    }

    private ControllerCommand copyCommand(final ControllerCommand source) {
        final ControllerCommand copy = new ControllerCommand();
        copy.setId(source.getId());
        copy.setControllerId(source.getControllerId());
        copy.setRequestedStatus(source.getRequestedStatus());
        copy.setRequestedOutputLevel(source.getRequestedOutputLevel());
        copy.setResultStatus(source.getResultStatus());
        copy.setResultMessage(source.getResultMessage());
        copy.setSensimulResponseCode(source.getSensimulResponseCode());
        copy.setCreatedAt(source.getCreatedAt());
        return copy;
    }
}
