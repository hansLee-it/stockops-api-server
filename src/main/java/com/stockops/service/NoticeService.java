package com.stockops.service;

import com.stockops.entity.Notice;
import com.stockops.entity.NoticeType;
import com.stockops.exception.ResourceNotFoundException;
import com.stockops.repository.NoticeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public List<Notice> getActiveNotices() {
        return noticeRepository.findByActiveTrueOrderByCreatedAtDesc();
    }

    public List<Notice> getNoticesByType(NoticeType type) {
        return noticeRepository.findByActiveTrueAndTypeOrderByCreatedAtDesc(type);
    }

    public Notice createNotice(String title, String content, NoticeType type, Long createdBy) {
        Notice notice = new Notice();
        notice.setTitle(title);
        notice.setContent(content);
        notice.setType(type != null ? type : NoticeType.SYSTEM);
        notice.setCreatedBy(createdBy);
        notice.setActive(true);
        return noticeRepository.save(notice);
    }

    public Notice updateNotice(Long id, String title, String content, NoticeType type, Boolean active) {
        Notice notice = noticeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Notice not found: " + id));
        if (title != null) notice.setTitle(title);
        if (content != null) notice.setContent(content);
        if (type != null) notice.setType(type);
        if (active != null) notice.setActive(active);
        return noticeRepository.save(notice);
    }

    public void deleteNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Notice not found: " + id));
        noticeRepository.delete(notice);
    }

    public List<Notice> getAllNotices() {
        return noticeRepository.findAll();
    }

    public NoticeService(final NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

}