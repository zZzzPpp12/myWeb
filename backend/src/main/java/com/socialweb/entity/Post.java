package com.socialweb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@Getter
@Setter
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PostType type = PostType.ARTICLE;

    /** 逗号分隔的标签字符串 */
    @Column(length = 500)
    private String tags;

    @Column(nullable = false)
    private long viewCount = 0;

    @Column(nullable = false)
    private long likeCount = 0;

    /** 反对数（知乎式赞赏+反对双向评价） */
    @Column(nullable = false)
    private long dislikeCount = 0;

    @Column(nullable = false)
    private long commentCount = 0;

    @Column(nullable = false)
    private long bookmarkCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
