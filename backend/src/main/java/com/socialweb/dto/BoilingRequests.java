package com.socialweb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BoilingRequests {

    public static class Create {
        @NotBlank(message = "内容不能为空")
        @Size(max = 500, message = "沸点内容最长 500 字")
        public String content;

        @Size(max = 512, message = "图片链接过长")
        public String imageUrl;
    }
}