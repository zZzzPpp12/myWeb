package com.socialweb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 沸点收藏 */
@Entity
@Table(name = "boiling_bookmark",
        uniqueConstraints = @UniqueConstraint(name = "uk_boiling_bm", columnNames = {"user_id", "boiling_id"}))
@Getter
@Setter
public class BoilingBookmark {

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