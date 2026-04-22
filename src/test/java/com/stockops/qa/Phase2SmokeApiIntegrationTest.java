package com.stockops.qa;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockops.auth.LoginRequest;
import com.stockops.repository.ai.AIRecommendationRepository;
import com.stockops.service.ai.AIRecommendationService;
import com.stockops.service.analytics.AnalyticsAggregationService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end API smoke coverage for scoped analytics, AI approval, and export authorization.
 *
 * @author StockOps Team
 * @since 2.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class Phase2SmokeApiIntegrationTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 5, 1);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Phase2QaFixtureFactory fixtureFactory;

    @Autowired
    private AnalyticsAggregationService analyticsAggregationService;

    @Autowired
    private AIRecommendationService aiRecommendationService;

    @Autowired
    private AIRecommendationRepository aiRecommendationRepository;

    /**
     * Verifies the happy path from scoped login to analytics visibility, recommendation approval, and export access.
     *
     * @throws Exception when response JSON parsing fails
     */
    @Test
    void scopedHappyPathCreatesDraftPurchaseOrderAndExports() throws Exception {
        final var fixture = fixtureFactory.seedPhase2Flow();
        refreshAnalytics();
        aiRecommendationService.generateRecommendationsForBusinessDate(BUSINESS_DATE);

        final String scopedToken = loginAndExtractToken(fixture.scopedUser().getEmail(), "Password123!");
        final Long recommendationId = aiRecommendationRepository.findByBusinessDate(BUSINESS_DATE).getFirst().getId();

        final ResponseEntity<String> analyticsResponse = exchange(
                "/api/v1/analytics/stock-aging?centerId=" + fixture.center().getId() + "&warehouseId=" + fixture.primaryWarehouse().getId(),
                HttpMethod.GET,
                scopedToken,
                null,
                String.class);
        assertThat(analyticsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(analyticsResponse.getBody()).path("summary").path("totalAvailableQuantity").asInt()).isGreaterThan(0);

        final ResponseEntity<String> approvalResponse = exchange(
                "/api/v1/ai/recommendations/" + recommendationId + "/approve",
                HttpMethod.POST,
                scopedToken,
                null,
                String.class);
        final JsonNode approvedPayload = objectMapper.readTree(approvalResponse.getBody());
        assertThat(approvalResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approvedPayload.path("status").asText()).isEqualTo("APPROVED_TO_DRAFT");
        assertThat(approvedPayload.path("approvedPurchaseOrderId").asLong()).isPositive();

        final long approvedPurchaseOrderId = approvedPayload.path("approvedPurchaseOrderId").asLong();
        final ResponseEntity<String> purchaseOrderResponse = exchange(
                "/api/v1/purchase-orders/" + approvedPurchaseOrderId,
                HttpMethod.GET,
                scopedToken,
                null,
                String.class);
        assertThat(purchaseOrderResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(purchaseOrderResponse.getBody()).path("status").asText()).isEqualTo("DRAFT");

        final ResponseEntity<byte[]> exportResponse = exchange(
                "/api/v1/reports/analytics/fill-rate/pdf?centerId=" + fixture.center().getId() + "&warehouseId=" + fixture.primaryWarehouse().getId(),
                HttpMethod.GET,
                scopedToken,
                null,
                byte[].class);
        assertThat(exportResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(exportResponse.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(exportResponse.getBody()).isNotEmpty();
    }

    /**
     * Verifies that direct out-of-scope report access is rejected with HTTP 403.
     */
    @Test
    void outOfScopeExportIsRejected() {
        final var fixture = fixtureFactory.seedPhase2Flow();
        refreshAnalytics();

        final String scopedToken = loginAndExtractToken(fixture.scopedUser().getEmail(), "Password123!");

        final ResponseEntity<String> forbiddenResponse = exchange(
                "/api/v1/reports/analytics/fill-rate/pdf?centerId=" + fixture.center().getId() + "&warehouseId=" + fixture.secondaryWarehouse().getId(),
                HttpMethod.GET,
                scopedToken,
                null,
                String.class);

        assertThat(forbiddenResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private void refreshAnalytics() {
        analyticsAggregationService.refreshRange(BUSINESS_DATE.minusDays(28), BUSINESS_DATE);
    }

    private String loginAndExtractToken(final String email, final String password) {
        final ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/api/v1/auth/login",
                new LoginRequest(email, password),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        try {
            return objectMapper.readTree(response.getBody()).path("accessToken").asText();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse login response", exception);
        }
    }

    private <T> ResponseEntity<T> exchange(final String path,
                                           final HttpMethod method,
                                           final String token,
                                           final Object body,
                                           final Class<T> responseType) {
        final HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }

        return restTemplate.exchange(baseUrl() + path, method, new HttpEntity<>(body, headers), responseType);
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }
}
