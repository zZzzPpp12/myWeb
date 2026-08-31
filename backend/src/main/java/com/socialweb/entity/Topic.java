package com.socialweb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 话题（融合知乎「话题」与掘金「标签」的独立实体） */
@Entity
@Table(name = "topics")
@Getter
@Setter
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 16)
    private String emoji;

    @Column(nullable = false)
    private long followerCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}