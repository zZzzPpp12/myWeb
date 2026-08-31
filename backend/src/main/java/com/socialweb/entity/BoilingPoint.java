package com.socialweb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 沸点：掘金式短内容动态流 */
@Entity
@Table(name = "boiling_point")
@Getter
@Setter
public class BoilingPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(length = 512)
    private String imageUrl;

    @Column(nullable = false)
    private long likeCount = 0;

    @Column(nullable = false)
    private long commentCount = 0;

    @Column(nullable = false)
    private long bookmarkCount = 0;

    /** 转发数 */
    @Column(nullable = false)
    private long shareCount = 0;

    /** 圈子/话题标记（原型「请选择圈子」，存话题名，可空） */
    @Column(length = 64)
    private String circle;

    /** 内容中的 @提及 用户 id，逗号分隔 */
    @Column(length = 500)
    private String mentionedUserIds;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}