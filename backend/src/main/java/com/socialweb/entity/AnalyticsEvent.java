package com.socialweb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 用户行为埋点事件（互动数据指标跟踪） */
@Entity
@Table(name = "analytics_event", indexes = {
        @Index(name = "idx_ae_action", columnList = "action"),
        @Index(name = "idx_ae_user_action", columnList = "user_id, action")
})
@Getter
@Setter
public class AnalyticsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    /** 事件名：boiling_view / boiling_like / boiling_comment / boiling_share / boiling_bookmark / comment_like / user_follow ... */
    @Column(nullable = false, length = 48)
    private String action;

    /** 目标对象类型与 id（可空） */
    @Column(length = 32)
    private String targetType;

    private Long targetId;

    /** 附加信息 JSON 字符串 */
    @Column(length = 500)
    private String extra;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}