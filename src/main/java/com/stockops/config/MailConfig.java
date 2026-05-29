package com.stockops.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Mail configuration that only activates when {@code spring.mail.host} is set.
 * <p>
 * Prevents application startup failure when no mail server is configured
 * in production or local environments.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Configuration
@ConditionalOnProperty(name = "spring.mail.host")
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(
            @Value("${spring.mail.host}") final String host,
            @Value("${spring.mail.port:25}") final int port,
            @Value("${spring.mail.username:}") final String username,
            @Value("${spring.mail.password:}") final String password,
            @Value("${spring.mail.protocol:smtp}") final String protocol) {
        final JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        sender.setProtocol(protocol);
        return sender;
    }
}
