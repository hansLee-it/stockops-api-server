package com.stockops.ai.bedrock.agent;

/**
 * Result of a single agent tool invocation dispatched by {@link AgentToolDispatcher}.
 *
 * @param toolName     name of the tool that was called
 * @param success      true if the tool executed without error
 * @param resultJson   JSON-serialized result payload (null on failure)
 * @param errorMessage human-readable error description (null on success)
 * @author StockOps Team
 * @since 2.0
 */
public record AgentToolResult(
        String toolName,
        boolean success,
        String resultJson,
        String errorMessage) {

    /**
     * Convenience factory for a successful result.
     */
    public static AgentToolResult success(final String toolName, final String resultJson) {
        return new AgentToolResult(toolName, true, resultJson, null);
    }

    /**
     * Convenience factory for a failed result.
     */
    public static AgentToolResult failure(final String toolName, final String errorMessage) {
        return new AgentToolResult(toolName, false, null, errorMessage);
    }
}
