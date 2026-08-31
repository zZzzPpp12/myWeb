package com.socialweb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class PostRequests {

    public static class Create {
        @NotBlank(message = "标题不能为空")
        @Size(max = 200, message = "标题过长")
        public String title;

        @NotBlank(message = "内容不能为空")
        public String content;

        @NotNull(message = "类型不能为空")
        public String type;

        @Size(max = 5, message = "最多 5 个标签")
        public List<String> tags;
    }

    public static class Update {
        @NotBlank(message = "标题不能为空")
        @Size(max = 200, message = "标题过长")
        public String title;

        @NotBlank(message = "内容不能为空")
        public String content;

        public List<String> tags;
    }

    public static class Comment {
        @NotBlank(message = "评论内容不能为空")
        @Size(max = 2000, message = "评论内容过长")
        public String content;

        public Long parentId;
    }
}
