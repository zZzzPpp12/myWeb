package com.socialweb.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PostDetail extends PostSummary {

    private String content;
    /** 当前登录用户是否关注了作者 */
    private Boolean followed;
}
