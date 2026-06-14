package com.stockops.environment.command;

import com.stockops.entity.ControllerCommand;
import com.stockops.entity.ControllerCommandResultStatus;
import com.stockops.repository.ControllerCommandRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finalizes controller commands from ACKs and times out un-acked ones.
 *
 * <p>ACK handling is idempotent (matching command → ACKED/FAILED), so it is safe even if multiple
 * load-balanced instances receive the same ACK over MQTT. A scheduled sweep marks SENT commands
 * older than the ack timeout as TIMEOUT.
 *
 * @author StockOps Team
 * @since 2.6
 */
@Service
public class ControllerCommandAckService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerCommandAckService.class);

    private final ControllerCommandRepository commandRepository;
    private final CommandMessagingProperties properties;

    public ControllerCommandAckService(final ControllerCommandRepository commandRepository,
                                       final CommandMessagingProperties properties) {
        this.commandRepository = commandRepository;
        this.properties = properties;
    }

    /**
     * Applies an ACK to its command. No-op when the correlation id is unknown or the command is
     * already in a terminal state (idempotent).
     *
     * @param ack ACK payload echoed by Sensimul
     */
    @Transactional
    public void applyAck(final ControllerCommandAck ack) {
        if (ack == null || ack.correlationId() == null || ack.correlationId().isBlank()) {
            LOGGER.warn("Ignoring controller ACK without correlationId");
            return;
        }
        final ControllerCommand command = commandRepository.findByCorrelationId(ack.correlationId()).orElse(null);
        if (command == null) {
            LOGGER.warn("No controller command for ACK correlationId={}", ack.correlationId());
            return;
        }
        if (isTerminal(command.getResultStatus())) {
            return;
        }
        final boolean applied = "APPLIED".equalsIgnoreCase(ack.resultStatus());
        command.setResultStatus(applied ? ControllerCommandResultStatus.ACKED : ControllerCommandResultStatus.FAILED);
        command.setSensimulResponseCode(ack.resultCode());
        command.setResultMessage(ack.message());
        commandRepository.save(command);
        LOGGER.debug("Controller command {} -> {} (correlationId={})",
                command.getId(), command.getResultStatus(), ack.correlationId());
    }

    /**
     * Marks SENT commands older than the ack timeout as TIMEOUT. Runs every 30s; the staleness
     * threshold itself is {@code stockops.command-messaging.ack-timeout}.
     */
    @Scheduled(fixedDelay = 30_000L)
    @Transactional
    public void sweepTimeouts() {
        if (!properties.isEnabled()) {
            return;
        }
        final Instant cutoff = Instant.now().minus(properties.getAckTimeout());
        final List<ControllerCommand> stale = commandRepository.findByResultStatusAndCreatedAtBefore(
                ControllerCommandResultStatus.SENT, cutoff);
        for (final ControllerCommand command : stale) {
            command.setResultStatus(ControllerCommandResultStatus.TIMEOUT);
            command.setResultMessage("ACK 미수신으로 타임아웃 처리되었습니다");
            commandRepository.save(command);
            LOGGER.warn("Controller command {} timed out (no ACK within {})",
                    command.getId(), properties.getAckTimeout());
        }
    }

    private boolean isTerminal(final ControllerCommandResultStatus status) {
        return status == ControllerCommandResultStatus.ACKED
                || status == ControllerCommandResultStatus.FAILED
                || status == ControllerCommandResultStatus.TIMEOUT
                || status == ControllerCommandResultStatus.APPLIED;
    }
}
