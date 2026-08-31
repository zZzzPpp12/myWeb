package com.socialweb.web;

import com.socialweb.dto.NotificationDto;
import com.socialweb.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Page<NotificationDto> list(@PageableDefault(size = 20) Pageable pageable) {
        return notificationService.list(AuthHelper.requireUser(), pageable);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("count", notificationService.unreadCount(AuthHelper.requireUser()));
    }

    @PostMapping("/read-all")
    public Map<String, Object> readAll() {
        notificationService.readAll(AuthHelper.requireUser());
        return Map.of("message", "全部已读");
    }
}
