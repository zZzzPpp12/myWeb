package com.socialweb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 64)
    private String nickname;

    @Column(length = 512)
    private String avatar;

    @Column(length = 500)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role = Role.USER;

    /** 声望值（知乎盐值 + 掘金掘力值融合），驱动等级徽章 */
    @Column(nullable = false)
    private int reputation = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
