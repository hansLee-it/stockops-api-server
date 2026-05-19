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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * End-to-end API smoke coverage for scoped analytics, AI approval, and export authorization.
 *
 * @author StockOps Team
 * @since 2.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class Phase2SmokeApiIntegrationTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 5, 1);

    @Autowired
    private MockMvc mockMvc;

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

        final MockHttpServletResponse analyticsResponse = exchange(
                "/api/v1/analytics/stock-aging?centerId=" + fixture.center().getId() + "&warehouseId=" + fixture.primaryWarehouse().getId(),
                HttpMethod.GET,
                scopedToken,
                null);
        assertThat(analyticsResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(objectMapper.readTree(analyticsResponse.getContentAsString()).path("summary").path("totalAvailableQuantity").asInt()).isGreaterThan(0);

        final MockHttpServletResponse approvalResponse = exchange(
                "/api/v1/ai/recommendations/" + recommendationId + "/approve",
                HttpMethod.POST,
                scopedToken,
                null);
        final JsonNode approvedPayload = objectMapper.readTree(approvalResponse.getContentAsString());
        assertThat(approvalResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(approvedPayload.path("status").asText()).isEqualTo("APPROVED_TO_DRAFT");
        assertThat(approvedPayload.path("approvedPurchaseOrderId").asLong()).isPositive();

        final long approvedPurchaseOrderId = approvedPayload.path("approvedPurchaseOrderId").asLong();
        final MockHttpServletResponse purchaseOrderResponse = exchange(
                "/api/v1/purchase-orders/" + approvedPurchaseOrderId,
                HttpMethod.GET,
                scopedToken,
                null);
        assertThat(purchaseOrderResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(purchaseOrderResponse.getContentAsString()).contains("\"status\":\"DRAFT\"");

        final MockHttpServletResponse exportResponse = exchange(
                "/api/v1/reports/analytics/fill-rate/pdf?centerId=" + fixture.center().getId() + "&warehouseId=" + fixture.primaryWarehouse().getId(),
                HttpMethod.GET,
                scopedToken,
                null);
        assertThat(exportResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(exportResponse.getContentType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
        assertThat(exportResponse.getContentAsByteArray()).isNotEmpty();
    }

    /**
     * Verifies that direct out-of-scope report access is rejected with HTTP 403.
     */
    @Test
    void outOfScopeExportIsRejected() {
        final var fixture = fixtureFactory.seedPhase2Flow();
        refreshAnalytics();

        final String scopedToken = loginAndExtractToken(fixture.scopedUser().getEmail(), "Password123!");

        final MockHttpServletResponse forbiddenResponse = exchange(
                "/api/v1/reports/analytics/fill-rate/pdf?centerId=" + fixture.secondaryWarehouse().getCenter().getId() + "&warehouseId=" + fixture.secondaryWarehouse().getId(),
                HttpMethod.GET,
                scopedToken,
                null);

        assertThat(forbiddenResponse.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    private void refreshAnalytics() {
        analyticsAggregationService.refreshRange(BUSINESS_DATE.minusDays(28), BUSINESS_DATE);
    }

    private String loginAndExtractToken(final String email, final String password) {
        final LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        final MockHttpServletResponse response = exchange("/api/v1/auth/login", HttpMethod.POST, null, request);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());

        try {
            return objectMapper.readTree(response.getContentAsString()).path("accessToken").asText();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse login response", exception);
        }
    }

    private MockHttpServletResponse exchange(final String path,
                                             final HttpMethod method,
                                             final String token,
                                             final Object body) {
        final MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .request(method, path)
                .contentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            requestBuilder.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            try {
                requestBuilder.content(objectMapper.writeValueAsBytes(body));
            } catch (Exception exception) {
                throw new IllegalStateException("Unable to serialize request body", exception);
            }
        }

        try {
            final MvcResult result = mockMvc.perform(requestBuilder).andReturn();
            return result.getResponse();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to execute mock request", exception);
        }
    }
}
