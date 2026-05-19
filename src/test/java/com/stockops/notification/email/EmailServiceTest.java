package com.stockops.notification.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.internet.MimeMessage;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private TemplateEngine templateEngine;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        templateEngine = templateEngine();
        emailService = new EmailService(mailSender, templateEngine);
        System.setProperty("email.enabled", "true");
    }

    @AfterEach
    void clearEmailSendingFlag() {
        System.clearProperty("email.enabled");
    }

    @Test
    void sendWeeklyReportProcessesTemplateAndSends() {
        final MimeMessage mimeMessage = mockMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        final EmailService.WeeklyReportData data = new EmailService.WeeklyReportData(
                "Test Center", "2026-04-01", "2026-04-07", 100, 80, 5.0, 3.0,
                List.of(new EmailService.ProductStats("Product A", 50, "EA"))
        );

        emailService.sendWeeklyReport("test@example.com", data);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendAlertProcessesAlertTemplate() {
        final MimeMessage mimeMessage = mockMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendAlert("admin@example.com", "High Temperature", "Warehouse 1: 35C detected", "WARNING");

        verify(mailSender).send(mimeMessage);
    }

    private MimeMessage mockMimeMessage() {
        return new MimeMessage((jakarta.mail.Session) null);
    }

    private TemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");

        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
