package com.socialweb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 沸点评论 点赞/点踩记录（互斥） */
@Entity
@Table(name = "boiling_comment_vote",
        uniqueConstraints = @UniqueConstraint(name = "uk_bc_vote", columnNames = {"user_id", "comment_id"}))
@Getter
@Setter
public class BoilingCommentVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id")
    private BoilingComment comment;

    /** true=赞 false=踩 */
    @Column(nullable = false)
    private boolean up;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}