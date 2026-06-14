package com.stockops.environment.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockops.entity.ControllerCommand;
import com.stockops.entity.ControllerCommandResultStatus;
import com.stockops.repository.ControllerCommandRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link ControllerCommandAckService}.
 *
 * @author StockOps Team
 * @since 2.6
 */
@ExtendWith(MockitoExtension.class)
class ControllerCommandAckServiceTest {

    @Mock
    private ControllerCommandRepository commandRepository;

    private final CommandMessagingProperties properties = new CommandMessagingProperties();
    private ControllerCommandAckService ackService;

    @BeforeEach
    void setUp() {
        ackService = new ControllerCommandAckService(commandRepository, properties);
    }

    @Test
    void applyAckMarksAckedOnApplied() {
        final ControllerCommand command = sentCommand("corr-1");
        when(commandRepository.findByCorrelationId("corr-1")).thenReturn(Optional.of(command));

        ackService.applyAck(new ControllerCommandAck("corr-1", "APPLIED", "OK", "applied"));

        assertThat(command.getResultStatus()).isEqualTo(ControllerCommandResultStatus.ACKED);
        assertThat(command.getSensimulResponseCode()).isEqualTo("OK");
        verify(commandRepository).save(command);
    }

    @Test
    void applyAckMarksFailedOnNonApplied() {
        final ControllerCommand command = sentCommand("corr-2");
        when(commandRepository.findByCorrelationId("corr-2")).thenReturn(Optional.of(command));

        ackService.applyAck(new ControllerCommandAck("corr-2", "FAILED", "INVALID", "bad output"));

        assertThat(command.getResultStatus()).isEqualTo(ControllerCommandResultStatus.FAILED);
    }

    @Test
    void applyAckIsIdempotentForTerminalCommand() {
        final ControllerCommand command = sentCommand("corr-3");
        command.setResultStatus(ControllerCommandResultStatus.ACKED);
        when(commandRepository.findByCorrelationId("corr-3")).thenReturn(Optional.of(command));

        ackService.applyAck(new ControllerCommandAck("corr-3", "APPLIED", "OK", null));

        verify(commandRepository, never()).save(any());
    }

    @Test
    void applyAckIgnoresUnknownCorrelationId() {
        when(commandRepository.findByCorrelationId("nope")).thenReturn(Optional.empty());

        ackService.applyAck(new ControllerCommandAck("nope", "APPLIED", "OK", null));

        verify(commandRepository, never()).save(any());
    }

    @Test
    void sweepTimeoutsMarksStaleSentCommands() {
        properties.setEnabled(true);
        final ControllerCommand stale = sentCommand("corr-old");
        when(commandRepository.findByResultStatusAndCreatedAtBefore(
                eq(ControllerCommandResultStatus.SENT), any(Instant.class))).thenReturn(List.of(stale));

        ackService.sweepTimeouts();

        assertThat(stale.getResultStatus()).isEqualTo(ControllerCommandResultStatus.TIMEOUT);
        verify(commandRepository).save(stale);
    }

    @Test
    void sweepTimeoutsNoOpWhenDisabled() {
        ackService.sweepTimeouts();

        verify(commandRepository, never()).findByResultStatusAndCreatedAtBefore(any(), any());
    }

    private ControllerCommand sentCommand(final String correlationId) {
        final ControllerCommand command = new ControllerCommand();
        command.setId(1L);
        command.setControllerId(10L);
        command.setCorrelationId(correlationId);
        command.setResultStatus(ControllerCommandResultStatus.SENT);
        return command;
    }
}
