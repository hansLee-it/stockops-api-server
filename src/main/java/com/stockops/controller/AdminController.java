package com.stockops.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "message", "Admin stats endpoint"
        ));
    }

    @GetMapping("/menus")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getMenus() {
        return ResponseEntity.ok(Map.of(
            "menus", java.util.List.of(
                Map.of("path", "/admin", "label", "대시보드"),
                Map.of("path", "/admin/notices", "label", "공지 관리"),
                Map.of("path", "/admin/audit-logs", "label", "감사 로그"),
                Map.of("path", "/admin/menus", "label", "메뉴 관리")
            )
        ));
    }
}