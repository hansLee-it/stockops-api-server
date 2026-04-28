package com.stockops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.stockops.notification.sms.SmsGateway;
import com.stockops.notification.sms.SmsResult;
import com.stockops.notification.sms.SmsSendHistory;
import com.stockops.notification.sms.SmsSendHistoryRepository;
import com.stockops.notification.sms.SmsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SmsServiceTest {

    @Mock
    private SmsGateway smsGateway;

    @Mock
    private SmsSendHistoryRepository historyRepository;

    @InjectMocks
    private SmsService smsService;

    @Test
    void sendSucceedsOnFirstAttempt() {
        when(smsGateway.send("+821012345678", "Hello"))
                .thenReturn(SmsResult.ok("msg-123"));
        when(historyRepository.save(any(SmsSendHistory.class)))
                .thenAnswer(i -> i.getArgument(0));

        final SmsResult result = smsService.send("+821012345678", "Hello");

        assertThat(result.success()).isTrue();
        assertThat(result.messageId()).isEqualTo("msg-123");
    }

    @Test
    void sendRetriesAndReturnsFailure() {
        when(smsGateway.send("+821012345678", "Hello"))
                .thenReturn(SmsResult.failure("Network error"));
        when(historyRepository.save(any(SmsSendHistory.class)))
                .thenAnswer(i -> i.getArgument(0));

        final SmsResult result = smsService.send("+821012345678", "Hello");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("Network error");
    }
}
