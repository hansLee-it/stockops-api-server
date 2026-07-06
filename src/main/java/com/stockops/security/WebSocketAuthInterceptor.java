package com.stockops.security;

import com.stockops.repository.WarehouseRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    /** Global firehose carrying every warehouse's stock events; global scope only. */
    private static final String STOCK_TOPIC = "/topic/stock";

    /** Stock topics are partitioned per warehouse: {@code /topic/stock/{warehouseId}}. */
    private static final String STOCK_TOPIC_PREFIX = "/topic/stock/";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final WarehouseRepository warehouseRepository;

    public WebSocketAuthInterceptor(final JwtTokenProvider jwtTokenProvider,
                                    final UserDetailsService userDetailsService,
                                    final WarehouseRepository warehouseRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.warehouseRepository = warehouseRepository;
    }

    @Override
    public Message<?> preSend(final Message<?> message, final MessageChannel channel) {
        final StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }

        return message;
    }

    private void authenticate(final StompHeaderAccessor accessor) {
        final String token = extractToken(accessor);

        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new MessageDeliveryException("Invalid or missing JWT token");
        }

        final String email = jwtTokenProvider.extractEmail(token);
        final UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (!userDetails.isEnabled()) {
            throw new MessageDeliveryException("Account is disabled");
        }

        final UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        // Bind the authenticated principal to the STOMP session; do not mutate the global
        // SecurityContextHolder, which is request-scoped and would leak across sessions.
        accessor.setUser(authentication);
    }

    private void authorizeSubscription(final StompHeaderAccessor accessor) {
        final String destination = accessor.getDestination();
        if (destination == null
                || !(STOCK_TOPIC.equals(destination) || destination.startsWith(STOCK_TOPIC_PREFIX))) {
            return;
        }

        final ScopeAccessProfile profile = resolveScopeProfile(accessor);
        if (profile == null) {
            throw new MessageDeliveryException("Authentication required for subscription");
        }

        if (STOCK_TOPIC.equals(destination)) {
            // The firehose carries every warehouse's events, so only global scope may subscribe.
            if (!profile.global()) {
                throw new MessageDeliveryException("Access denied for destination: " + destination);
            }
            return;
        }

        final Long warehouseId = parseWarehouseId(destination);
        if (warehouseId == null || !canAccessWarehouse(profile, warehouseId)) {
            throw new MessageDeliveryException("Access denied for destination: " + destination);
        }
    }

    /**
     * Mirrors {@code ScopeGuard.canAccessWarehouse}: a warehouse is visible through an explicit
     * warehouse assignment or through a center assignment covering its parent center.
     */
    private boolean canAccessWarehouse(final ScopeAccessProfile profile, final Long warehouseId) {
        if (profile.canAccessWarehouse(warehouseId)) {
            return true;
        }
        return warehouseRepository.findById(warehouseId)
                .map(warehouse -> warehouse.getCenter() != null
                        && profile.centerIds().contains(warehouse.getCenter().getId()))
                .orElse(false);
    }

    private ScopeAccessProfile resolveScopeProfile(final StompHeaderAccessor accessor) {
        final Principal principal = accessor.getUser();
        if (principal instanceof UsernamePasswordAuthenticationToken authentication
                && authentication.getPrincipal() instanceof ScopedUserDetails scopedUserDetails) {
            return scopedUserDetails.getScopeAccessProfile();
        }
        return null;
    }

    private Long parseWarehouseId(final String destination) {
        final String suffix = destination.substring(STOCK_TOPIC_PREFIX.length());
        try {
            return Long.valueOf(suffix);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String extractToken(final StompHeaderAccessor accessor) {
        final List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            final String header = authHeaders.get(0);
            if (header.startsWith("Bearer ")) {
                return header.substring(7);
            }
            return header;
        }

        final List<String> authTokenHeaders = accessor.getNativeHeader("auth-token");
        if (authTokenHeaders != null && !authTokenHeaders.isEmpty()) {
            return authTokenHeaders.get(0);
        }

        return null;
    }
}
