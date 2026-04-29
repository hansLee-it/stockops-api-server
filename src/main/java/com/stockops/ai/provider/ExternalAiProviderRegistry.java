package com.stockops.ai.provider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Registry that auto-discovers all {@link ExternalAiProvider} Spring beans
 * and indexes them by {@link ExternalAiProvider#getProviderId()}.
 *
 * <p>Uses constructor injection of a list of all ExternalAiProvider beans,
 * then builds a lookup map keyed by provider id string.</p>
 *
 * @author StockOps Team
 * @since 2.0
 * @see WebhookProviderRegistry
 */
@Slf4j
@Component
public class ExternalAiProviderRegistry {

    private final Map<String, ExternalAiProvider> providers = new HashMap<>();

    /**
     * Constructs the registry by collecting all ExternalAiProvider beans.
     *
     * @param providerList all ExternalAiProvider implementations registered as Spring beans
     */
    public ExternalAiProviderRegistry(final List<ExternalAiProvider> providerList) {
        for (ExternalAiProvider provider : providerList) {
            String id = provider.getProviderId();
            if (providers.containsKey(id)) {
                log.warn("Duplicate ExternalAiProvider id '{}': overwriting {} with {}",
                        id, providers.get(id).getClass().getSimpleName(),
                        provider.getClass().getSimpleName());
            }
            providers.put(id, provider);
            log.info("Registered ExternalAiProvider: id={}, class={}",
                    id, provider.getClass().getSimpleName());
        }
    }

    /**
     * Looks up a provider by its identifier.
     *
     * @param providerId the provider id string (e.g. "gemini")
     * @return matching provider, or empty if not registered
     */
    public Optional<ExternalAiProvider> getProvider(final String providerId) {
        return Optional.ofNullable(providers.get(providerId));
    }

    /**
     * Returns all registered provider ids.
     *
     * @return set of registered provider id strings
     */
    public java.util.Set<String> getRegisteredIds() {
        return providers.keySet();
    }
}
