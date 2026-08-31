package com.socialweb.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationDto {

    private Long id;
    private String type;
    private boolean read;
    private UserSummary actor;
    private PostSummary post;
    private LocalDateTime createdAt;
}
