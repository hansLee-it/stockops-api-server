package com.stockops.service;

import com.stockops.notification.email.EmailService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
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
    }

    @Test
    void sendWeeklyReportInMockMode() {
        final EmailService.WeeklyReportData data = new EmailService.WeeklyReportData(
                "Test Center", "2026-04-21", "2026-04-27",
                100, 50, 10.0, -5.0,
                List.of(new EmailService.ProductStats("Product A", 20, "EA"))
        );

        emailService.sendWeeklyReport("test@example.com", data);
    }

    @Test
    void sendAlertInMockMode() {
        emailService.sendAlert("test@example.com", "Temperature Alert", "High temp", "CRITICAL");
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
