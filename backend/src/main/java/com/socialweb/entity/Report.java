package com.socialweb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 举报（沸点或沸点评论） */
@Entity
@Table(name = "report")
@Getter
@Setter
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 举报人 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    /** BOILING / BOILING_COMMENT */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ReportTargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    /** 举报理由 */
    @Column(length = 200)
    private String reason;

    /** PENDING / RESOLVED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public enum ReportTargetType { BOILING, BOILING_COMMENT }

    public enum ReportStatus { PENDING, RESOLVED }
}