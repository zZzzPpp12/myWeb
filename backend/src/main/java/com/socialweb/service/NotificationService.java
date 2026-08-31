package com.socialweb.service;

import com.socialweb.dto.NotificationDto;
import com.socialweb.entity.Notification;
import com.socialweb.entity.NotificationType;
import com.socialweb.entity.Post;
import com.socialweb.entity.User;
import com.socialweb.repository.NotificationRepository;
import com.socialweb.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final PostRepository postRepository;
    private final Mapper mapper;

    public NotificationService(NotificationRepository notificationRepository,
                               PostRepository postRepository,
                               Mapper mapper) {
        this.notificationRepository = notificationRepository;
        this.postRepository = postRepository;
        this.mapper = mapper;
    }

    /** 产生通知；收件人与触发者相同则跳过 */
    @Transactional
    public void create(User recipient, User actor, NotificationType type, Long postId) {
        if (recipient == null || actor == null || recipient.getId().equals(actor.getId())) return;
        Notification n = new Notification();
        n.setUser(recipient);
        n.setActor(actor);
        n.setType(type);
        n.setPostId(postId);
        notificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> list(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void readAll(Long userId) {
        notificationRepository.markAllRead(userId);
    }

    @Transactional(readOnly = true)
    public NotificationDto toDto(Notification n) {
        NotificationDto dto = new NotificationDto();
        dto.setId(n.getId());
        dto.setType(n.getType().name());
        dto.setRead(n.isRead());
        dto.setActor(mapper.toUserSummary(n.getActor()));
        dto.setCreatedAt(n.getCreatedAt());
        // MENTION / BOILING_* 通知无关联文章，仅文章类通知加载 post
        if (n.getPostId() != null && (n.getType() == NotificationType.LIKE
                || n.getType() == NotificationType.COMMENT
                || n.getType() == NotificationType.POST)) {
            Post p = postRepository.findById(n.getPostId()).orElse(null);
            if (p != null) {
                dto.setPost(mapper.toPostSummary(p, false, false));
            }
        }
        return dto;
    }
}
