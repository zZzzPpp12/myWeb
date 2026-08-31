package com.socialweb.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BoilingDto {

    private Long id;
    private String content;
    private String imageUrl;
    private long likeCount;
    private long commentCount;
    private boolean liked;
    private UserSummary author;
    private LocalDateTime createdAt;
}