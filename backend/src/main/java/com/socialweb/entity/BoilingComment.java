package com.socialweb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 沸点评论（掘金式沸点讨论区，支持两级嵌套） */
@Entity
@Table(name = "boiling_comment")
@Getter
@Setter
public class BoilingComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "boiling_id")
    private BoilingPoint boilingPoint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false, length = 1000)
    private String content;

    /** 父评论 id；null=主评论，非 null=回复（回复的回复统一挂到顶层，记录回复目标人） */
    private Long parentId;

    /** 回复目标用户（仅回复时记录，用于前端显示「A 回复 B」） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_user_id")
    private User replyToUser;

    /** @提及的用户 id，逗号分隔 */
    @Column(length = 500)
    private String mentionedUserIds;

    @Column(nullable = false)
    private long likeCount = 0;

    @Column(nullable = false)
    private long dislikeCount = 0;

    @Column(nullable = false)
    private boolean reported = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}