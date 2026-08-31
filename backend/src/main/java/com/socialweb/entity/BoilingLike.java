package com.socialweb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 沸点点赞记录 */
@Entity
@Table(name = "boiling_like",
        uniqueConstraints = @UniqueConstraint(name = "uk_boiling_like", columnNames = {"user_id", "boiling_id"}))
@Getter
@Setter
public class BoilingLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "boiling_id")
    private BoilingPoint boilingPoint;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}