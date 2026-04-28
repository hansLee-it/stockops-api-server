package com.stockops.notification.sms;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class SmsNotificationController {

    private final SmsService smsService;

    @PostMapping("/sms")
    public ResponseEntity<?> sendSms(@RequestBody SmsRequest request) {
        log.info("Received SMS request to {}: {}", request.phoneNumber(), request.message());
        SmsResult result = smsService.send(request.phoneNumber(), request.message());
        return ResponseEntity.ok(Map.of(
                "success", result.success(),
                "messageId", result.messageId() != null ? result.messageId() : "",
                "errorMessage", result.errorMessage() != null ? result.errorMessage() : ""
        ));
    }

    record SmsRequest(@NotBlank String phoneNumber, @NotBlank String message) {}
}