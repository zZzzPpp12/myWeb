package com.socialweb.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSummary {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    /** 声望值 */
    private int reputation;
    /** 等级（1-6） */
    private int level;
    /** 等级称号 */
    private String levelName;
    /** 是否在线（5 分钟内活跃） */
    private Boolean online;
}