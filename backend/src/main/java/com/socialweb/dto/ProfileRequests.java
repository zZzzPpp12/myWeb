package com.socialweb.dto;

import jakarta.validation.constraints.Size;

public class ProfileRequests {

    public static class Update {
        @Size(max = 64, message = "昵称过长")
        public String nickname;

        @Size(max = 512, message = "头像地址过长")
        public String avatar;

        @Size(max = 500, message = "简介过长")
        public String bio;
    }
}
