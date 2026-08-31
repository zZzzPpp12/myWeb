package com.socialweb.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CommentDto {

    private Long id;
    private Long postId;
    private String content;
    private Long parentId;
    private UserSummary author;
    private LocalDateTime createdAt;
    private List<CommentDto> replies = new ArrayList<>();
}
