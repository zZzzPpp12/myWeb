package com.socialweb.web;

import com.socialweb.security.CurrentUser;
import org.springframework.http.HttpStatus;

/** 控制器公共辅助 */
public final class AuthHelper {

    private AuthHelper() {
    }

    /** 返回当前登录用户 id，未登录抛 401 */
    public static Long requireUser() {
        Long uid = CurrentUser.get();
        if (uid == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return uid;
    }

    /** 返回当前登录用户 id，未登录返回 null */
    public static Long optionalUser() {
        return CurrentUser.get();
    }
}
