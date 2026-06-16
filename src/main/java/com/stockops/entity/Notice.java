package com.stockops.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

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

    /** Roles this notice is delivered to (webhook routing). Empty = broadcast to all role channels. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_roles", nullable = false, columnDefinition = "jsonb DEFAULT '[]'")
    private List<String> targetRoles = List.of();

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

    public List<String> getTargetRoles() {
        return this.targetRoles;
    }

    public void setTargetRoles(final List<String> targetRoles) {
        this.targetRoles = targetRoles == null ? List.of() : targetRoles;
    }
}