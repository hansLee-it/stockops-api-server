package com.stockops.environment.ingestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

/**
 * Polls Sensimul live telemetry from SQS and forwards it to the shared ingestion pipeline.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Component
public class SqsTelemetrySubscriber implements SmartInitializingSingleton {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqsTelemetrySubscriber.class);
    private static final String SNS_MESSAGE_FIELD = "Message";
    private static final int HTTP_FORBIDDEN = 403;
    private static final long ACCESS_DENIED_BACKOFF_MILLIS = 60000;

    private final SqsIngestionProperties properties;
    private final TelemetryIngestionService telemetryIngestionService;
    private final ObjectMapper objectMapper;
    private final AtomicLong errorCounter = new AtomicLong();

    private volatile boolean running;
    private ExecutorService executorService;
    private SqsClient sqsClient;

    /**
     * Creates the SQS telemetry subscriber.
     *
     * @param properties ingestion properties
     * @param telemetryIngestionService ingestion service
     * @param objectMapper jackson object mapper
     */
    public SqsTelemetrySubscriber(
            final SqsIngestionProperties properties,
            final TelemetryIngestionService telemetryIngestionService,
            final ObjectMapper objectMapper) {
        this.properties = properties;
        this.telemetryIngestionService = telemetryIngestionService;
        this.objectMapper = objectMapper;
    }

    /**
     * Starts the SQS polling loop after all singletons are initialized.
     */
    @Override
    public void afterSingletonsInstantiated() {
        if (!properties.isEnabled()) {
            LOGGER.info("SQS ingestion disabled by configuration");
            return;
        }
        if (!StringUtils.hasText(properties.getQueueUrl())) {
            LOGGER.warn("SQS ingestion enabled but no queue URL configured");
            return;
        }

        sqsClient = SqsClient.builder()
                .region(Region.of(properties.getRegion()))
                .build();
        running = true;
        executorService = Executors.newSingleThreadExecutor(daemonThreadFactory());
        executorService.submit(this::pollLoop);

        LOGGER.info(
                "SQS ingestion started queueUrl={} region={} maxMessages={} waitTimeSeconds={}",
                properties.getQueueUrl(),
                properties.getRegion(),
                properties.getMaxMessages(),
                properties.getWaitTimeSeconds());
    }

    @PreDestroy
    void shutdown() {
        running = false;
        if (executorService != null) {
            executorService.shutdownNow();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOGGER.debug("Timed out waiting for SQS ingestion worker to stop");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
        if (sqsClient != null) {
            sqsClient.close();
        }
    }

    private void pollLoop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                receiveAndProcessMessages();
            } catch (SqsException exception) {
                final long failures = errorCounter.incrementAndGet();
                if (exception.statusCode() == HTTP_FORBIDDEN) {
                    LOGGER.warn("SQS receive denied queueUrl={} statusCode={} failures={} message={}",
                            properties.getQueueUrl(), exception.statusCode(), failures, exception.getMessage());
                    sleepAfterError(ACCESS_DENIED_BACKOFF_MILLIS);
                } else {
                    LOGGER.warn("SQS receive failed queueUrl={} failures={}",
                            properties.getQueueUrl(), failures, exception);
                    sleepAfterError(properties.getErrorBackoffMillis());
                }
            } catch (RuntimeException exception) {
                final long failures = errorCounter.incrementAndGet();
                LOGGER.error("Unexpected SQS ingestion failure failures={}", failures, exception);
                sleepAfterError(properties.getErrorBackoffMillis());
            }
        }
    }

    private void receiveAndProcessMessages() {
        final ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(properties.getQueueUrl())
                .maxNumberOfMessages(clamp(properties.getMaxMessages(), 1, 10))
                .waitTimeSeconds(clamp(properties.getWaitTimeSeconds(), 0, 20))
                .visibilityTimeout(properties.getVisibilityTimeoutSeconds())
                .messageAttributeNames("All")
                .build();

        for (Message message : sqsClient.receiveMessage(request).messages()) {
            if (processMessage(message)) {
                deleteMessage(message);
            }
        }
    }

    private boolean processMessage(final Message message) {
        try {
            final SensimulPayload payload = normalizeFromAttributes(parsePayload(message.body()), message.messageAttributes());
            telemetryIngestionService.ingest(payload);
            return true;
        } catch (Exception exception) {
            final long failures = errorCounter.incrementAndGet();
            LOGGER.error("Failed to process SQS telemetry message messageId={} parseErrors={}",
                    message.messageId(), failures, exception);
            return false;
        }
    }

    private SensimulPayload parsePayload(final String body) throws IOException {
        try {
            return objectMapper.readValue(body, SensimulPayload.class);
        } catch (IOException directParseException) {
            final JsonNode root = objectMapper.readTree(body);
            final JsonNode snsMessage = root.get(SNS_MESSAGE_FIELD);
            if (snsMessage != null && snsMessage.isTextual()) {
                return objectMapper.readValue(snsMessage.asText(), SensimulPayload.class);
            }
            throw directParseException;
        }
    }

    private SensimulPayload normalizeFromAttributes(
            final SensimulPayload payload,
            final Map<String, MessageAttributeValue> attributes) {
        final String siteId = StringUtils.hasText(payload.siteId())
                ? payload.siteId()
                : stringAttribute(attributes, "siteId");
        final String sensorId = StringUtils.hasText(payload.sensorId())
                ? payload.sensorId()
                : stringAttribute(attributes, "sensorId");
        return new SensimulPayload(
                siteId,
                sensorId,
                payload.sensorType(),
                payload.valueKind(),
                payload.value(),
                payload.unit(),
                payload.status(),
                payload.timestamp(),
                payload.sequenceId(),
                payload.schemaVersion());
    }

    private String stringAttribute(final Map<String, MessageAttributeValue> attributes, final String key) {
        final MessageAttributeValue value = attributes.get(key);
        if (value == null) {
            return null;
        }
        return value.stringValue();
    }

    private void deleteMessage(final Message message) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(properties.getQueueUrl())
                .receiptHandle(message.receiptHandle())
                .build());
    }

    private void sleepAfterError(final long millis) {
        try {
            Thread.sleep(Duration.ofMillis(millis).toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }

    private ThreadFactory daemonThreadFactory() {
        return runnable -> {
            final Thread thread = new Thread(runnable, "stockops-sqs-ingestion");
            thread.setDaemon(true);
            return thread;
        };
    }
}
