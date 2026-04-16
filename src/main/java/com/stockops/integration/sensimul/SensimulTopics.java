package com.stockops.integration.sensimul;

import java.util.Optional;

/**
 * Canonical Sensimul MQTT topic helpers.
 *
 * @author StockOps Team
 * @since 1.0
 */
public final class SensimulTopics {

    public static final String TOPIC_BASE = "sensimul";

    private SensimulTopics() {
    }

    /**
     * Builds the canonical live telemetry topic.
     *
     * @param siteId site identifier
     * @param sensorId sensor identifier
     * @return live telemetry topic
     */
    public static String liveSensor(final String siteId, final String sensorId) {
        return TOPIC_BASE + "/sites/" + siteId + "/sensors/" + sensorId;
    }

    /**
     * Builds the canonical live controller topic.
     *
     * @param siteId site identifier
     * @param controllerId controller identifier
     * @return live controller topic
     */
    public static String liveController(final String siteId, final String controllerId) {
        return TOPIC_BASE + "/sites/" + siteId + "/controllers/" + controllerId;
    }

    /**
     * Returns the live telemetry topic filter.
     *
     * @return live telemetry filter
     */
    public static String liveSensorFilter() {
        return TOPIC_BASE + "/sites/+/sensors/+";
    }

    /**
     * Builds the one-shot test request topic.
     *
     * @param siteId site identifier
     * @param sensorId sensor identifier
     * @return test request topic
     */
    public static String testRequest(final String siteId, final String sensorId) {
        return TOPIC_BASE + "/tests/requests/sites/" + siteId + "/sensors/" + sensorId;
    }

    /**
     * Returns the one-shot test request filter.
     *
     * @return test request filter
     */
    public static String testRequestFilter() {
        return TOPIC_BASE + "/tests/requests/sites/+/sensors/+";
    }

    /**
     * Builds the one-shot test result topic.
     *
     * @param siteId site identifier
     * @param sensorId sensor identifier
     * @return test result topic
     */
    public static String testResult(final String siteId, final String sensorId) {
        return TOPIC_BASE + "/tests/results/sites/" + siteId + "/sensors/" + sensorId;
    }

    /**
     * Returns the one-shot test result filter.
     *
     * @return test result filter
     */
    public static String testResultFilter() {
        return TOPIC_BASE + "/tests/results/sites/+/sensors/+";
    }

    /**
     * Parses a live telemetry topic.
     *
     * @param topic topic value to parse
     * @return parsed topic when the topic matches the live telemetry contract
     */
    public static Optional<ParsedSensorTopic> parseLiveTopic(final String topic) {
        final String[] parts = topic.split("/");
        if (parts.length != 5) {
            return Optional.empty();
        }
        if (!TOPIC_BASE.equals(parts[0]) || !"sites".equals(parts[1]) || !"sensors".equals(parts[3])) {
            return Optional.empty();
        }
        return Optional.of(new ParsedSensorTopic(parts[2], parts[4]));
    }

    /**
     * Parses a live controller topic.
     *
     * @param topic topic value to parse
     * @return parsed topic when the topic matches the live controller contract
     */
    public static Optional<ParsedControllerTopic> parseLiveControllerTopic(final String topic) {
        final String[] parts = topic.split("/");
        if (parts.length != 5) {
            return Optional.empty();
        }
        if (!TOPIC_BASE.equals(parts[0]) || !"sites".equals(parts[1]) || !"controllers".equals(parts[3])) {
            return Optional.empty();
        }
        return Optional.of(new ParsedControllerTopic(parts[2], parts[4]));
    }

    /**
     * Parses a Sensimul test topic.
     *
     * @param topic topic value to parse
     * @return parsed topic when the topic matches the test request/result contract
     */
    public static Optional<ParsedTestTopic> parseTestTopic(final String topic) {
        final String[] parts = topic.split("/");
        if (parts.length != 7) {
            return Optional.empty();
        }
        if (!TOPIC_BASE.equals(parts[0]) || !"tests".equals(parts[1]) || !"sites".equals(parts[3])
                || !"sensors".equals(parts[5])) {
            return Optional.empty();
        }
        if (!"requests".equals(parts[2]) && !"results".equals(parts[2])) {
            return Optional.empty();
        }
        return Optional.of(new ParsedTestTopic(parts[2], parts[4], parts[6]));
    }
}
