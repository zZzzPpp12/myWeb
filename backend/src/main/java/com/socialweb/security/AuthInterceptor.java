package com.socialweb.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;
    private final jakarta.persistence.EntityManagerFactory emf;

    public AuthInterceptor(JwtService jwtService, jakarta.persistence.EntityManagerFactory emf) {
        this.jwtService = jwtService;
        this.emf = emf;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            Long uid = jwtService.parseUserId(header.substring(7).trim());
            if (uid != null) {
                CurrentUser.set(uid);
                touchActive(uid);
            }
        }
        return true;
    }

    /** 更新活跃时间（在线状态推断），异常不影响请求 */
    private void touchActive(Long uid) {
        try {
            jakarta.persistence.EntityManager em = emf.createEntityManager();
            try {
                em.getTransaction().begin();
                em.createQuery("update User u set u.lastActiveAt = :now where u.id = :uid")
                        .setParameter("now", java.time.LocalDateTime.now())
                        .setParameter("uid", uid)
                        .executeUpdate();
                em.getTransaction().commit();
            } finally {
                em.close();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUser.clear();
    }
}
