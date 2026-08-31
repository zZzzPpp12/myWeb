package com.socialweb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@Setter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 收件人 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    /** 触发者，可空 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationType type;

    private Long postId;

    @Column(name = "read_flag", nullable = false)
    private boolean read = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
