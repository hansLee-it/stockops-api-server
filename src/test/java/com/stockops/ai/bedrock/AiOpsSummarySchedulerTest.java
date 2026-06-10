package com.stockops.ai.bedrock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockops.ai.bedrock.dto.BedrockOpsSummaryResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiOpsSummarySchedulerTest {

    @Mock
    private BedrockAiFacade bedrockAiFacade;

    private AiOpsSummaryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new AiOpsSummaryScheduler(bedrockAiFacade);
    }

    @Test
    void generateDailyOpsSummaries_callsSummarizeOperationsForToday() {
        when(bedrockAiFacade.summarizeOperations(any(), isNull(), isNull()))
                .thenReturn(new BedrockOpsSummaryResponse(
                        null, null, null, "요약 내용", List.of(), List.of(), "MEDIUM", Instant.now()));

        scheduler.generateDailyOpsSummaries();

        verify(bedrockAiFacade, times(1)).summarizeOperations(any(), isNull(), isNull());
    }

    @Test
    void generateDailyOpsSummaries_whenSummarizeFails_doesNotThrow() {
        when(bedrockAiFacade.summarizeOperations(any(), isNull(), isNull()))
                .thenThrow(new RuntimeException("Bedrock unavailable"));

        // Must not propagate — scheduler swallows errors to keep the thread alive
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                scheduler.generateDailyOpsSummaries());
    }
}
