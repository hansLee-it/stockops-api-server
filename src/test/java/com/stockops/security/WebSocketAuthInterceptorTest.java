package com.stockops.security;

import com.stockops.entity.Center;
import com.stockops.entity.Warehouse;
import com.stockops.repository.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Verifies WebSocket SUBSCRIBE authorization isolates stock topics by warehouse scope (A-5).
 */
@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private MessageChannel channel;

    private WebSocketAuthInterceptor interceptor() {
        return new WebSocketAuthInterceptor(jwtTokenProvider, userDetailsService, warehouseRepository);
    }

    private ScopedUserDetails user(final ScopeAccessProfile profile) {
        return new ScopedUserDetails(
                1L,
                "user@stockops.com",
                "pw",
                true,
                List.of(new SimpleGrantedAuthority("INVENTORY_READ")),
                profile);
    }

    private ScopedUserDetails scopedUser(final Set<Long> warehouseIds) {
        return user(new ScopeAccessProfile(false, List.of(), Set.of(), warehouseIds));
    }

    private Message<byte[]> subscribeMessage(final ScopedUserDetails userDetails, final String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setUser(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
        accessor.setLeaveMutable(true);
        return org.springframework.messaging.support.MessageBuilder
                .createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void rejectsSubscriptionToOutOfScopeWarehouseTopic() {
        Message<byte[]> message = subscribeMessage(scopedUser(Set.of(1L)), "/topic/stock/99");

        assertThatThrownBy(() -> interceptor().preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void allowsSubscriptionToInScopeWarehouseTopic() {
        Message<byte[]> message = subscribeMessage(scopedUser(Set.of(1L)), "/topic/stock/1");

        Message<?> result = interceptor().preSend(message, channel);

        assertThat(result).isSameAs(message);
    }

    @Test
    void allowsFirehoseSubscriptionForGlobalScope() {
        ScopedUserDetails globalUser = user(new ScopeAccessProfile(true, List.of(), Set.of(), Set.of()));
        Message<byte[]> message = subscribeMessage(globalUser, "/topic/stock");

        Message<?> result = interceptor().preSend(message, channel);

        assertThat(result).isSameAs(message);
    }

    @Test
    void rejectsFirehoseSubscriptionForScopedUser() {
        Message<byte[]> message = subscribeMessage(scopedUser(Set.of(1L)), "/topic/stock");

        assertThatThrownBy(() -> interceptor().preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void allowsWarehouseTopicThroughParentCenterScope() {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(5L);
        Center center = new Center();
        center.setId(1L);
        warehouse.setCenter(center);
        when(warehouseRepository.findById(5L)).thenReturn(Optional.of(warehouse));

        ScopedUserDetails centerUser = user(new ScopeAccessProfile(false, List.of(), Set.of(1L), Set.of()));
        Message<byte[]> message = subscribeMessage(centerUser, "/topic/stock/5");

        Message<?> result = interceptor().preSend(message, channel);

        assertThat(result).isSameAs(message);
    }
}
