package com.stockops.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;
import java.time.Instant;

/**
 * System notice entity for admin announcements.
 */
@Entity
@Table(name = "notices")
@SQLRestriction("deleted = false")
public class Notice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NoticeType type = NoticeType.SYSTEM;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "notice_at")
    private Instant noticeAt;

    public Notice() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public NoticeType getType() {
        return this.type;
    }

    public void setType(final NoticeType type) {
        this.type = type;
    }

    public Boolean getActive() {
        return this.active;
    }

    public void setActive(final Boolean active) {
        this.active = active;
    }

    public Long getCreatedBy() {
        return this.createdBy;
    }

    public void setCreatedBy(final Long createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getNoticeAt() {
        return this.noticeAt;
    }

    public void setNoticeAt(final Instant noticeAt) {
        this.noticeAt = noticeAt;
    }
}