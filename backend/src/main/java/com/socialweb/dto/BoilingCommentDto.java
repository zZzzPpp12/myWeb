package com.socialweb.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class BoilingCommentDto {

    private Long id;
    private Long boilingId;
    private String content;
    private Long parentId;
    /** 被回复人摘要（仅回复时非空） */
    private UserSummary replyToUser;
    private UserSummary author;
    private long likeCount;
    private long dislikeCount;
    /** 当前用户投票：null 未投 / true 赞 / false 踩 */
    private Boolean myVote;
    private LocalDateTime createdAt;
    private List<BoilingCommentDto> replies = new ArrayList<>();
}