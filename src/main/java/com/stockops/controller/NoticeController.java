package com.stockops.controller;

import com.stockops.entity.Notice;
import com.stockops.entity.NoticeType;
import com.stockops.service.NoticeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notices")
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping("/active")
    public ResponseEntity<List<Notice>> getActiveNotices() {
        return ResponseEntity.ok(noticeService.getActiveNotices());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Notice>> getAllNotices() {
        return ResponseEntity.ok(noticeService.getAllNotices());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Notice> createNotice(@RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        String typeStr = (String) body.get("type");
        NoticeType type = typeStr != null ? NoticeType.valueOf(typeStr) : NoticeType.SYSTEM;
        Long createdBy = body.get("createdBy") != null ? ((Number) body.get("createdBy")).longValue() : null;
        Notice notice = noticeService.createNotice(title, content, type, createdBy);
        return ResponseEntity.ok(notice);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Notice> updateNotice(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        String typeStr = (String) body.get("type");
        Boolean active = body.get("active") != null ? (Boolean) body.get("active") : null;
        NoticeType type = typeStr != null ? NoticeType.valueOf(typeStr) : null;
        Notice notice = noticeService.updateNotice(id, title, content, type, active);
        return ResponseEntity.ok(notice);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return ResponseEntity.noContent().build();
    }

    public NoticeController(final NoticeService noticeService) {
        this.noticeService = noticeService;
    }

}