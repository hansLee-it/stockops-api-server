package com.stockops.ai.bedrock;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;

/**
 * Creates Bedrock Agent runtime clients. Extracted as a Spring bean so the
 * adapter's agent loop can be unit-tested with a mocked client.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Component
public class BedrockAgentRuntimeClientFactory {

    /**
     * Creates an async Bedrock Agent runtime client for the given region.
     * The async client is required because {@code invokeAgent} responses stream events.
     *
     * @param region AWS region id
     * @return async client; callers own closing it
     */
    public BedrockAgentRuntimeAsyncClient createAsyncClient(final String region) {
        return BedrockAgentRuntimeAsyncClient.builder()
                .region(Region.of(region))
                .build();
    }
}
