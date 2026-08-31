package com.socialweb.service;

import com.socialweb.dto.CommentDto;
import com.socialweb.dto.PostRequests;
import com.socialweb.entity.Comment;
import com.socialweb.entity.NotificationType;
import com.socialweb.entity.Post;
import com.socialweb.entity.User;
import com.socialweb.repository.CommentRepository;
import com.socialweb.repository.PostRepository;
import com.socialweb.repository.UserRepository;
import com.socialweb.web.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final Mapper mapper;

    public CommentService(CommentRepository commentRepository,
                          PostRepository postRepository,
                          UserRepository userRepository,
                          NotificationService notificationService,
                          Mapper mapper) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.mapper = mapper;
    }

    @Transactional
    public CommentDto add(Long postId, Long userId, PostRequests.Comment req) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "文章不存在"));
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));

        Comment c = new Comment();
        c.setPost(post);
        c.setAuthor(author);
        c.setContent(req.content.trim());

        if (req.parentId != null) {
            Comment parent = commentRepository.findById(req.parentId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "父评论不存在"));
            if (!parent.getPost().getId().equals(postId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "父评论不属于该文章");
            }
            // 仅支持一级回复：回复的回复挂到同一个顶层
            c.setParentId(parent.getParentId() != null ? parent.getParentId() : parent.getId());
            // 通知父评论作者（与文章作者不同才额外通知，避免重复）
            if (!parent.getAuthor().getId().equals(post.getAuthor().getId())) {
                notificationService.create(parent.getAuthor(), author, NotificationType.COMMENT, postId);
            }
        }
        Comment saved = commentRepository.save(c);
        // 通知文章作者（create 内部跳过自己给自己发通知）
        notificationService.create(post.getAuthor(), author, NotificationType.COMMENT, postId);
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);
        LevelSystem.award(author, LevelSystem.COMMENT_CREATE);
        return toDto(saved, List.of());
    }

    /** 顶层分页，replies 全量返回 */
    @Transactional(readOnly = true)
    public Page<CommentDto> list(Long postId, Pageable pageable) {
        Page<Comment> topPage = commentRepository.findByPostIdAndParentIdIsNullOrderByCreatedAtAsc(postId, pageable);
        List<Comment> tops = topPage.getContent();
        List<Long> topIds = tops.stream().map(Comment::getId).toList();
        Map<Long, List<Comment>> repliesMap = topIds.isEmpty() ? Map.of()
                : commentRepository.findByParentIdInOrderByCreatedAtAsc(topIds).stream()
                .collect(Collectors.groupingBy(Comment::getParentId));
        List<CommentDto> dtos = tops.stream()
                .map(c -> toDto(c, repliesMap.getOrDefault(c.getId(), List.of()).stream()
                        .map(r -> toDto(r, List.of())).toList()))
                .toList();
        return new PageImpl<>(dtos, pageable, topPage.getTotalElements());
    }

    private CommentDto toDto(Comment c, List<CommentDto> replies) {
        CommentDto d = new CommentDto();
        d.setId(c.getId());
        d.setPostId(c.getPost().getId());
        d.setContent(c.getContent());
        d.setParentId(c.getParentId());
        d.setAuthor(mapper.toUserSummary(c.getAuthor()));
        d.setCreatedAt(c.getCreatedAt());
        d.setReplies(replies);
        return d;
    }
}
