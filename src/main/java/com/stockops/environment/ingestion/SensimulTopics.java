package com.stockops.environment.ingestion;

/**
 * Sensimul MQTT topic contract helpers.
 *
 * @author StockOps Team
 * @since 1.0
 */
public final class SensimulTopics {

    private static final String TOPIC_BASE = "sensimul";

    private SensimulTopics() {
    }

    /**
     * Builds the canonical live telemetry topic for a sensor.
     *
     * @param siteId site identifier
     * @param sensorId sensor identifier
     * @return MQTT topic
     */
    public static String liveSensorTopic(final String siteId, final String sensorId) {
        return TOPIC_BASE + "/sites/" + siteId + "/sensors/" + sensorId;
    }

    /**
     * Returns the wildcard filter for all live sensor messages.
     *
     * @return topic filter
     */
    public static String liveSensorFilter() {
        return TOPIC_BASE + "/sites/+/sensors/+";
    }

    /**
     * Parses a live topic into its site and sensor identifiers.
     *
     * @param topic MQTT topic
     * @return parsed topic parts when valid
     */
    public static ParsedLiveTopic parseLiveTopic(final String topic) {
        if (topic == null) {
            return ParsedLiveTopic.invalid();
        }

        final String[] parts = topic.split("/");
        if (parts.length != 5) {
            return ParsedLiveTopic.invalid();
        }
        if (!TOPIC_BASE.equals(parts[0]) || !"sites".equals(parts[1]) || !"sensors".equals(parts[3])) {
            return ParsedLiveTopic.invalid();
        }
        return new ParsedLiveTopic(parts[2], parts[4], true);
    }

    /**
     * Parsed live topic result.
     *
     * @param siteId site identifier
     * @param sensorId sensor identifier
     * @param valid parse success flag
     */
    public record ParsedLiveTopic(String siteId, String sensorId, boolean valid) {

        private static ParsedLiveTopic invalid() {
            return new ParsedLiveTopic(null, null, false);
        }
    }
}
