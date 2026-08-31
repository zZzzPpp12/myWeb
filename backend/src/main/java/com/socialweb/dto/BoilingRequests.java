package com.socialweb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class BoilingRequests {

    public static class Create {
        @NotBlank(message = "内容不能为空")
        @Size(max = 1000, message = "沸点内容最长 1000 字")
        public String content;

        @Size(max = 512, message = "图片链接过长")
        public String imageUrl;

        /** 圈子（话题名），可空 */
        @Size(max = 64, message = "圈子名过长")
        public String circle;
    }

    public static class Comment {
        @NotBlank(message = "评论内容不能为空")
        @Size(max = 1000, message = "评论内容最长 1000 字")
        public String content;

        public Long parentId;
    }

    public static class Report {
        @NotBlank(message = "举报理由不能为空")
        @Size(max = 200, message = "理由最长 200 字")
        public String reason;
    }

    public static class Analytics {
        @NotBlank(message = "action 不能为空")
        @Size(max = 48)
        public String action;

        @Size(max = 32)
        public String targetType;

        public Long targetId;

        @Size(max = 500)
        public String extra;
    }
}