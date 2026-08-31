package com.socialweb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthRequests {

    public static class Register {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 32, message = "用户名长度需在 3-32 之间")
        public String username;

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度需在 6-64 之间")
        public String password;

        @NotBlank(message = "昵称不能为空")
        @Size(max = 64, message = "昵称过长")
        public String nickname;
    }

    public static class Login {
        @NotBlank(message = "用户名不能为空")
        public String username;

        @NotBlank(message = "密码不能为空")
        public String password;
    }
}
