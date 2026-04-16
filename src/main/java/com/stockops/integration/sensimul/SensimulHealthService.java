package com.stockops.integration.sensimul;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Health-check service for the external Sensimul simulator.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Service
public class SensimulHealthService {

    private final RestTemplate sensimulRestTemplate;

    private final SensimulProperties properties;

    /**
     * Creates the service.
     *
     * @param sensimulRestTemplate configured Sensimul HTTP client
     * @param properties Sensimul configuration properties
     */
    public SensimulHealthService(final RestTemplate sensimulRestTemplate, final SensimulProperties properties) {
        this.sensimulRestTemplate = sensimulRestTemplate;
        this.properties = properties;
    }

    /**
     * Checks whether Sensimul responds successfully to its health endpoint.
     *
     * @return true when the external service returns HTTP 200-range status, otherwise false
     */
    public boolean isHealthy() {
        try {
            final ResponseEntity<Void> response = sensimulRestTemplate.getForEntity(buildUri("healthz"), Void.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException ex) {
            return false;
        }
    }

    private URI buildUri(final String... pathSegments) {
        final StringBuilder builder = new StringBuilder(properties.getNormalizedBaseUrl());
        for (String pathSegment : pathSegments) {
            builder.append('/').append(pathSegment);
        }
        return URI.create(builder.toString());
    }
}
