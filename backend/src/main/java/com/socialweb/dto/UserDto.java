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
    /** 是否在线（5 分钟内活跃） */
    private Boolean online;
    /** 互动统计：收到的赞数 */
    private Long likesReceived;
    /** 互动统计：沸点数 */
    private Long boilingsCount;
    /** 最近活跃时间 */
    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;
}
