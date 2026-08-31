package com.socialweb.dto;

import lombok.Data;

@Data
public class TopicDto {

    private Long id;
    private String name;
    private String description;
    private String emoji;
    private Long followerCount;
    private Long postCount;
    private Boolean followed;
}