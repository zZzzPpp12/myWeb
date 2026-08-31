package com.socialweb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_follow",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_follow", columnNames = {"follower_id", "followee_id"}))
@Getter
@Setter
public class UserFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id")
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "followee_id")
    private User followee;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
