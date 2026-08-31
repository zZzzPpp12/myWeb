package com.socialweb.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;

    public AuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            Long uid = jwtService.parseUserId(header.substring(7).trim());
            if (uid != null) {
                CurrentUser.set(uid);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUser.clear();
    }
}
