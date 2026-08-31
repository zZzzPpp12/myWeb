package com.socialweb.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostSummary {

    private Long id;
    private String title;
    private String excerpt;
    private String type;
    private List<String> tags;
    private UserSummary author;
    private long viewCount;
    private long likeCount;
    private long dislikeCount;
    /** 认同值 = 赞同 - 反对 */
    private long score;
    private long commentCount;
    private long bookmarkCount;
    private boolean liked;
    private boolean downvoted;
    private boolean bookmarked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
