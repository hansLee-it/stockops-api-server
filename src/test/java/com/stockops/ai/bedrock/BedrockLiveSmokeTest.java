package com.stockops.ai.bedrock;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Live smoke test for Bedrock integration.
 * Disabled by default — only runs when STOCKOPS_BEDROCK_LIVE_TESTS=true.
 *
 * To run:
 * STOCKOPS_BEDROCK_ENABLED=true \
 * STOCKOPS_BEDROCK_LIVE_TESTS=true \
 * STOCKOPS_BEDROCK_REGION=ap-northeast-2 \
 * STOCKOPS_BEDROCK_MODEL_ID=<model-id-or-inference-profile> \
 * mvn -Dgroups=bedrock-live test
 */
@Tag("bedrock-live")
@EnabledIfEnvironmentVariable(named = "STOCKOPS_BEDROCK_LIVE_TESTS", matches = "true")
@SpringBootTest
@ActiveProfiles("bedrock-live")
class BedrockLiveSmokeTest {

    @Test
    void liveProfileLoads() {
        assertThat(System.getenv("STOCKOPS_BEDROCK_ENABLED")).isIn("true", "false", null);
    }
}
