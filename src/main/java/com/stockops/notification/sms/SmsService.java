package com.stockops.notification.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.stockops.notification.sms.SmsSendHistory.SmsSendStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Orchestrates SMS dispatch with retry logic and history persistence.
 * Wraps SmsGateway.send() and saves every attempt to SmsSendHistory.
 *
 * @author StockOps Team
 * @since 1.0
 * @see SmsGateway
 * @see SmsSendHistory
 */
@Service
public class SmsService {

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;

    private final SmsGateway smsGateway;
    private final SmsSendHistoryRepository historyRepository;

    /**
     * Sends an SMS with automatic retry on failure.
     *
     * @param phoneNumber destination in E.164 format
     * @param message     text content
     * @return SmsResult from final attempt (success or after all retries exhausted)
     */
    @Transactional
    public SmsResult send(final String phoneNumber, final String message) {
        SmsResult lastResult = null;

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            lastResult = smsGateway.send(phoneNumber, message);

            if (lastResult.success()) {
                persistHistory(phoneNumber, message, SmsSendStatus.SENT, lastResult.messageId(), null, attempt);
                return lastResult;
            }

            if (attempt < MAX_RETRY_ATTEMPTS) {
                long backoffMs = INITIAL_BACKOFF_MS * (1 << (attempt - 1));
                long jitter = ThreadLocalRandom.current().nextLong(0, backoffMs / 2);
                long waitMs = backoffMs + jitter;
                log.warn("SMS send attempt {} failed for {}, retrying in {}ms: {}",
                        attempt, phoneNumber, waitMs, lastResult.errorMessage());

                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        persistHistory(phoneNumber, message, SmsSendStatus.FAILURE, null,
                lastResult != null ? lastResult.errorMessage() : "Unknown error",
                MAX_RETRY_ATTEMPTS);

        return lastResult != null ? lastResult : SmsResult.failure("All retry attempts exhausted");
    }

    private void persistHistory(final String phoneNumber, final String message,
                                final SmsSendStatus status, final String messageId,
                                final String errorMessage, final int attemptCount) {
        SmsSendHistory history = new SmsSendHistory();
        history.setPhoneNumber(phoneNumber);
        history.setMessage(message);
        history.setStatus(status);
        history.setMessageId(messageId);
        history.setErrorMessage(errorMessage);
        history.setAttemptCount(attemptCount);
        if (status == SmsSendStatus.SENT) {
            history.setSentAt(Instant.now());
        }
        historyRepository.save(history);
    }

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    public SmsService(final SmsGateway smsGateway, final SmsSendHistoryRepository historyRepository) {
        this.smsGateway = smsGateway;
        this.historyRepository = historyRepository;
    }
}