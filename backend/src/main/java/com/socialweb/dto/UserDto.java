package com.socialweb.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String bio;
    private String role;
    private Long followersCount;
    private Long followingCount;
    private Long postsCount;
    private Boolean followed;
    private int reputation;
    private int level;
    private String levelName;
    private LocalDateTime createdAt;
}
