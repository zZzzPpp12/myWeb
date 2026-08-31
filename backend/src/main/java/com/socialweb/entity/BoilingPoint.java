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

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}