package com.stockops.audit;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Exposes Spring beans to JPA entity listeners.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(final ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    /**
     * Returns a bean by type when the application context is available.
     *
     * @param beanType bean class
     * @param <T> bean type
     * @return resolved bean or {@code null}
     */
    public static <T> T getBean(final Class<T> beanType) {
        if (applicationContext == null) {
            return null;
        }
        return applicationContext.getBean(beanType);
    }
}
