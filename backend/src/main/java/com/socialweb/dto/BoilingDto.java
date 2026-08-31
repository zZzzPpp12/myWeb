package com.socialweb.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BoilingDto {

    private Long id;
    private String content;
    private String imageUrl;
    /** 圈子（话题名） */
    private String circle;
    private long likeCount;
    private long commentCount;
    private long bookmarkCount;
    private long shareCount;
    private boolean liked;
    private boolean bookmarked;
    private UserSummary author;
    private LocalDateTime createdAt;
}