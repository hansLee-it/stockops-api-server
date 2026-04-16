package com.stockops.integration.sensimul;

import java.time.Duration;
import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client configuration for Sensimul integration calls.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Configuration
public class SensimulHttpClientConfiguration {

    /**
     * Builds the dedicated RestTemplate used by the Sensimul adapter.
     *
     * @param properties Sensimul connection properties
     * @return configured RestTemplate
     */
    @Bean
    public RestTemplate sensimulRestTemplate(final SensimulProperties properties) {
        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(toMillis(properties.getConnectTimeout()));
        requestFactory.setReadTimeout(toMillis(properties.getReadTimeout()));

        final RestTemplate restTemplate = new RestTemplate(requestFactory);
        restTemplate.setErrorHandler(noOpErrorHandler());
        return restTemplate;
    }

    private int toMillis(final Duration duration) {
        return Math.toIntExact(duration.toMillis());
    }

    private ResponseErrorHandler noOpErrorHandler() {
        return new ResponseErrorHandler() {
            @Override
            public boolean hasError(final ClientHttpResponse response) throws IOException {
                return false;
            }

            @Override
            public void handleError(final ClientHttpResponse response) throws IOException {
                // No-op: the Sensimul adapter inspects non-2xx responses directly.
            }
        };
    }
}
