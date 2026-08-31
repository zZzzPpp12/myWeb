package com.socialweb.service;

import com.socialweb.dto.PostDetail;
import com.socialweb.dto.PostRequests;
import com.socialweb.dto.PostSummary;
import com.socialweb.entity.*;
import com.socialweb.repository.PostBookmarkRepository;
import com.socialweb.repository.PostDislikeRepository;
import com.socialweb.repository.PostLikeRepository;
import com.socialweb.repository.PostRepository;
import com.socialweb.repository.TopicFollowRepository;
import com.socialweb.repository.UserFollowRepository;
import com.socialweb.repository.UserRepository;
import com.socialweb.web.ApiException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserFollowRepository followRepository;
    private final PostLikeRepository likeRepository;
    private final PostDislikeRepository dislikeRepository;
    private final PostBookmarkRepository bookmarkRepository;
    private final TopicFollowRepository topicFollowRepository;
    private final com.socialweb.repository.CommentRepository commentRepository;
    private final NotificationService notificationService;
    private final Mapper mapper;

    public PostService(PostRepository postRepository,
                       UserRepository userRepository,
                       UserFollowRepository followRepository,
                       PostLikeRepository likeRepository,
                       PostDislikeRepository dislikeRepository,
                       PostBookmarkRepository bookmarkRepository,
                       TopicFollowRepository topicFollowRepository,
                       com.socialweb.repository.CommentRepository commentRepository,
                       NotificationService notificationService,
                       Mapper mapper) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.likeRepository = likeRepository;
        this.dislikeRepository = dislikeRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.topicFollowRepository = topicFollowRepository;
        this.commentRepository = commentRepository;
        this.notificationService = notificationService;
        this.mapper = mapper;
    }

    /** 热度算法：(like*1 + comment*2 + bookmark*3 + view*0.1) / (小时龄+2)^1.2 */
    public static double hotScore(Post p, LocalDateTime now) {
        double numerator = p.getLikeCount() * 1 + p.getCommentCount() * 2
                + p.getBookmarkCount() * 3 + p.getViewCount() * 0.1;
        double hours = Math.max(0, Duration.between(p.getCreatedAt(), now).toMinutes() / 60.0);
        return numerator / Math.pow(hours + 2, 1.2);
    }

    @Transactional
    public PostDetail create(Long authorId, PostRequests.Create req) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
        Post p = new Post();
        p.setAuthor(author);
        p.setTitle(req.title.trim());
        p.setContent(req.content);
        p.setType(parseType(req.type));
        p.setTags(normalizeTags(req.tags));
        Post saved = postRepository.save(p);
        // 发布给关注者产生 POST 通知
        for (UserFollow f : followRepository.findByFollowerId(authorId)) {
            notificationService.create(f.getFollower(), author, NotificationType.POST, saved.getId());
        }
        LevelSystem.award(author, LevelSystem.POST_CREATE);
        return mapper.toPostDetail(saved, false, false, false);
    }

    @Transactional(readOnly = true)
    public Page<PostSummary> list(String feed, String tag, Long authorId, Long viewerId, Pageable pageable) {
        if (authorId != null) {
            Page<Post> page = postRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, pageable);
            return new PageImpl<>(mapper.toSummaries(page.getContent(), viewerId), pageable, page.getTotalElements());
        }
        if (tag != null && !tag.isBlank()) {
            return latestByTag(tag, viewerId, pageable);
        }
        String f = feed == null || feed.isBlank() ? "latest" : feed.toLowerCase();
        return switch (f) {
            case "hot" -> hot(viewerId, pageable);
            case "following" -> following(viewerId, pageable);
            default -> latest(viewerId, pageable);
        };
    }

    private Page<PostSummary> latest(Long viewerId, Pageable pageable) {
        Page<Post> page = postRepository.findAll(pageable);
        return new PageImpl<>(mapper.toSummaries(page.getContent(), viewerId), pageable, page.getTotalElements());
    }

    private Page<PostSummary> latestByTag(String tag, Long viewerId, Pageable pageable) {
        Specification<Post> spec = (root, q, cb) -> cb.or(
                cb.equal(root.get("tags"), tag),
                cb.like(root.get("tags"), tag + ",%"),
                cb.like(root.get("tags"), "%," + tag + ",%"),
                cb.like(root.get("tags"), "%," + tag));
        Page<Post> page = postRepository.findAll(spec, pageable);
        return new PageImpl<>(mapper.toSummaries(page.getContent(), viewerId), pageable, page.getTotalElements());
    }

    private Page<PostSummary> hot(Long viewerId, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        List<Post> sorted = postRepository.findAll().stream()
                .sorted(Comparator.comparingDouble((Post p) -> hotScore(p, now)).reversed())
                .toList();
        return manualPage(sorted, viewerId, pageable);
    }

    private Page<PostSummary> following(Long viewerId, Pageable pageable) {
        if (viewerId == null) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        Set<Long> followingIds = followRepository.findByFollowerId(viewerId).stream()
                .map(f -> f.getFollowee().getId())
                .collect(Collectors.toSet());
        LocalDateTime now = LocalDateTime.now();
        List<Post> sorted = postRepository.findAll().stream()
                .filter(p -> followingIds.contains(p.getAuthor().getId()))
                .sorted(Comparator.comparing((Post p) -> p.getCreatedAt()).reversed())
                .toList();
        return manualPage(sorted, viewerId, pageable);
    }

    private Page<PostSummary> manualPage(List<Post> all, Long viewerId, Pageable pageable) {
        int start = (int) Math.min(pageable.getOffset(), all.size());
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<Post> slice = all.subList(start, end);
        return new PageImpl<>(mapper.toSummaries(slice, viewerId), pageable, all.size());
    }

    /** 个性化推荐：基于点赞/收藏过的 tag 匹配 + 热度回退 */
    @Transactional(readOnly = true)
    public List<PostSummary> recommended(Long viewerId, int limit) {
        LocalDateTime now = LocalDateTime.now();
        List<Post> all = postRepository.findAll();
        if (viewerId == null) {
            return mapper.toSummaries(all.stream()
                    .sorted(Comparator.comparingDouble((Post p) -> hotScore(p, now)).reversed())
                    .limit(limit).toList(), null);
        }
        Set<String> interested = new HashSet<>();
        for (PostLike l : likeRepository.findByUserId(viewerId)) {
            interested.addAll(Mapper.parseTags(l.getPost().getTags()));
        }
        for (PostBookmark b : bookmarkRepository.findByUserId(viewerId)) {
            interested.addAll(Mapper.parseTags(b.getPost().getTags()));
        }
        // 已关注话题也纳入兴趣标签（知乎式关注话题影响推荐）
        interested.addAll(topicFollowRepository.findTopicNamesByUserId(viewerId));
        Comparator<Post> byHot = Comparator.comparingDouble((Post p) -> hotScore(p, now)).reversed();
        List<Post> matched = all.stream()
                .filter(p -> !p.getAuthor().getId().equals(viewerId))
                .filter(p -> !likeRepository.existsByUserIdAndPostId(viewerId, p.getId()))
                .filter(p -> Mapper.parseTags(p.getTags()).stream().anyMatch(interested::contains))
                .sorted(byHot)
                .limit(limit)
                .toList();
        if (matched.size() < limit) {
            Set<Long> picked = matched.stream().map(Post::getId).collect(Collectors.toSet());
            List<Post> filler = all.stream()
                    .filter(p -> !picked.contains(p.getId()))
                    .filter(p -> !p.getAuthor().getId().equals(viewerId))
                    .sorted(byHot)
                    .limit(limit - matched.size())
                    .toList();
            List<Post> combined = new ArrayList<>(matched);
            combined.addAll(filler);
            matched = combined;
        }
        return mapper.toSummaries(matched, viewerId);
    }

    @Transactional
    public PostDetail detail(Long id, Long viewerId) {
        Post p = postRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "文章不存在"));
        p.setViewCount(p.getViewCount() + 1);
        postRepository.save(p);
        boolean followed = viewerId != null && mapper.follows(viewerId, p.getAuthor().getId());
        return mapper.toPostDetail(p, mapper.liked(viewerId, id), mapper.bookmarked(viewerId, id),
                mapper.downvoted(viewerId, id), followed);
    }

    @Transactional
    public PostDetail update(Long id, Long userId, PostRequests.Update req) {
        Post p = postRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "文章不存在"));
        if (!p.getAuthor().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "只有作者可以修改");
        }
        p.setTitle(req.title.trim());
        p.setContent(req.content);
        if (req.tags != null) p.setTags(normalizeTags(req.tags));
        postRepository.save(p);
        return mapper.toPostDetail(p, mapper.liked(userId, id), mapper.bookmarked(userId, id),
                mapper.downvoted(userId, id), false);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Post p = postRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "文章不存在"));
        if (!p.getAuthor().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "只有作者可以删除");
        }
        commentRepository.deleteByPostId(id);
        likeRepository.deleteByPostId(id);
        dislikeRepository.deleteByPostId(id);
        bookmarkRepository.deleteByPostId(id);
        postRepository.delete(p);
    }

    @Transactional
    public Map<String, Boolean> toggleLike(Long userId, Long postId) {
        Post p = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "文章不存在"));
        User u = userRepository.getReferenceById(userId);
        Optional<PostLike> existing = likeRepository.findByUserIdAndPostId(userId, postId);
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            p.setLikeCount(Math.max(0, p.getLikeCount() - 1));
            postRepository.save(p);
            return Map.of("liked", false);
        }
        // 赞同与反对互斥：赞同前先取消反对
        dislikeRepository.findByUserIdAndPostId(userId, postId).ifPresent(d -> {
            dislikeRepository.delete(d);
            p.setDislikeCount(Math.max(0, p.getDislikeCount() - 1));
        });
        PostLike l = new PostLike();
        l.setUser(u);
        l.setPost(p);
        likeRepository.save(l);
        p.setLikeCount(p.getLikeCount() + 1);
        postRepository.save(p);
        LevelSystem.award(p.getAuthor(), LevelSystem.RECEIVE_LIKE);
        notificationService.create(p.getAuthor(), u, NotificationType.LIKE, postId);
        return Map.of("liked", true);
    }

    @Transactional
    public Map<String, Boolean> toggleDislike(Long userId, Long postId) {
        Post p = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "文章不存在"));
        User u = userRepository.getReferenceById(userId);
        Optional<PostDislike> existing = dislikeRepository.findByUserIdAndPostId(userId, postId);
        if (existing.isPresent()) {
            dislikeRepository.delete(existing.get());
            p.setDislikeCount(Math.max(0, p.getDislikeCount() - 1));
            postRepository.save(p);
            return Map.of("downvoted", false);
        }
        // 反对与赞同互斥：反对前先取消赞同
        likeRepository.findByUserIdAndPostId(userId, postId).ifPresent(l -> {
            likeRepository.delete(l);
            p.setLikeCount(Math.max(0, p.getLikeCount() - 1));
        });
        PostDislike d = new PostDislike();
        d.setUser(u);
        d.setPost(p);
        dislikeRepository.save(d);
        p.setDislikeCount(p.getDislikeCount() + 1);
        postRepository.save(p);
        LevelSystem.award(p.getAuthor(), LevelSystem.RECEIVE_DISLIKE);
        return Map.of("downvoted", true);
    }

    @Transactional
    public Map<String, Boolean> toggleBookmark(Long userId, Long postId) {
        Post p = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "文章不存在"));
        User u = userRepository.getReferenceById(userId);
        Optional<PostBookmark> existing = bookmarkRepository.findByUserIdAndPostId(userId, postId);
        if (existing.isPresent()) {
            bookmarkRepository.delete(existing.get());
            p.setBookmarkCount(Math.max(0, p.getBookmarkCount() - 1));
            postRepository.save(p);
            return Map.of("bookmarked", false);
        }
        PostBookmark b = new PostBookmark();
        b.setUser(u);
        b.setPost(p);
        bookmarkRepository.save(b);
        p.setBookmarkCount(p.getBookmarkCount() + 1);
        postRepository.save(p);
        return Map.of("bookmarked", true);
    }

    @Transactional(readOnly = true)
    public Page<PostSummary> search(String q, Long viewerId, Pageable pageable) {
        if (q == null || q.isBlank()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        String like = "%" + q.trim() + "%";
        Specification<Post> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.like(root.get("title"), like));
            preds.add(cb.like(root.get("content"), like));
            return cb.or(preds.toArray(new Predicate[0]));
        };
        Page<Post> page = postRepository.findAll(spec, pageable);
        return new PageImpl<>(mapper.toSummaries(page.getContent(), viewerId), pageable, page.getTotalElements());
    }

    private static PostType parseType(String type) {
        try {
            return PostType.valueOf(type.toUpperCase());
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "类型必须是 ARTICLE 或 QUESTION");
        }
    }

    static String normalizeTags(List<String> tags) {
        if (tags == null) return "";
        return tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .limit(5)
                .collect(Collectors.joining(","));
    }
}
