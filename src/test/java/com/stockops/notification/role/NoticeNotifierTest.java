package com.stockops.notification.role;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.stockops.entity.Notice;
import com.stockops.entity.NoticeType;
import com.stockops.notification.webhook.WebhookPayload;
import com.stockops.notification.webhook.WebhookService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link NoticeNotifier} role-based notice routing.
 *
 * @author StockOps Team
 * @since 2.3
 */
@ExtendWith(MockitoExtension.class)
class NoticeNotifierTest {

    @Mock
    private RoleWebhookConfigRepository repository;

    @Mock
    private WebhookService webhookService;

    @InjectMocks
    private NoticeNotifier notifier;

    @Test
    void dispatchesToTargetRoleChannels() {
        when(repository.findByRoleInAndEnabledTrue(List.of("ADMIN")))
                .thenReturn(List.of(config("ADMIN", "admin-url")));

        notifier.dispatch(notice("점검 공지", List.of("ADMIN")));

        verify(webhookService).send(eq("TEAMS"), eq("admin-url"), any(WebhookPayload.class));
        verify(webhookService, times(1)).send(any(), any(), any(WebhookPayload.class));
    }

    @Test
    void broadcastsToAllEnabledChannelsDedupedByUrlWhenNoTargetRoles() {
        when(repository.findByEnabledTrue()).thenReturn(List.of(
                config("ADMIN", "url-1"),
                config("MANAGER", "url-1"),
                config("STAFF", "url-2")));

        notifier.dispatch(notice("전체 공지", List.of()));

        verify(webhookService).send(eq("TEAMS"), eq("url-1"), any(WebhookPayload.class));
        verify(webhookService).send(eq("TEAMS"), eq("url-2"), any(WebhookPayload.class));
        verify(webhookService, times(2)).send(any(), any(), any(WebhookPayload.class));
    }

    @Test
    void skipsWhenNoChannelConfigured() {
        when(repository.findByRoleInAndEnabledTrue(List.of("ADMIN"))).thenReturn(List.of());

        notifier.dispatch(notice("공지", List.of("ADMIN")));

        verifyNoInteractions(webhookService);
    }

    private Notice notice(final String title, final List<String> roles) {
        final Notice notice = new Notice();
        notice.setId(1L);
        notice.setTitle(title);
        notice.setType(NoticeType.SYSTEM);
        notice.setTargetRoles(roles);
        return notice;
    }

    private RoleWebhookConfig config(final String role, final String url) {
        final RoleWebhookConfig config = new RoleWebhookConfig();
        config.setRole(role);
        config.setWebhookUrl(url);
        config.setEnabled(true);
        return config;
    }
}
