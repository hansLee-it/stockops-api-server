package com.stockops.ai.bedrock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockops.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiRagRateLimiterTest {

    private AiRagRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        // Small limit for fast testing
        rateLimiter = new AiRagRateLimiter(3);
    }

    @Test
    void checkRagLimit_withinLimit_doesNotThrow() {
        rateLimiter.checkRagLimit("user@example.com");
        rateLimiter.checkRagLimit("user@example.com");
        rateLimiter.checkRagLimit("user@example.com");
        // 3 requests — exactly at limit, no exception
    }

    @Test
    void checkRagLimit_exceededLimit_throwsRateLimitExceededException() {
        rateLimiter.checkRagLimit("user@example.com");
        rateLimiter.checkRagLimit("user@example.com");
        rateLimiter.checkRagLimit("user@example.com");

        assertThatThrownBy(() -> rateLimiter.checkRagLimit("user@example.com"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("3회/분");
    }

    @Test
    void checkRagLimit_differentUsers_haveIndependentBuckets() {
        // user1 exhausts their quota
        rateLimiter.checkRagLimit("user1@example.com");
        rateLimiter.checkRagLimit("user1@example.com");
        rateLimiter.checkRagLimit("user1@example.com");

        // user2 should still be able to make requests
        rateLimiter.checkRagLimit("user2@example.com");
        rateLimiter.checkRagLimit("user2@example.com");
    }

    @Test
    void checkRagLimit_user1Exhausted_doesNotAffectUser2() {
        for (int i = 0; i < 3; i++) {
            rateLimiter.checkRagLimit("user1@example.com");
        }

        assertThatThrownBy(() -> rateLimiter.checkRagLimit("user1@example.com"))
                .isInstanceOf(RateLimitExceededException.class);

        // user2 is unaffected
        rateLimiter.checkRagLimit("user2@example.com");
    }

    @Test
    void getRequestsPerMinute_returnsConfiguredLimit() {
        assertThat(rateLimiter.getRequestsPerMinute()).isEqualTo(3);
    }
}
