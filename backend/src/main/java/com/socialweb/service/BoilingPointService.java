package com.socialweb.service;

import com.socialweb.dto.BoilingCommentDto;
import com.socialweb.dto.BoilingDto;
import com.socialweb.dto.BoilingRequests;
import com.socialweb.entity.*;
import com.socialweb.repository.*;
import com.socialweb.web.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** 沸点：掘金式短内容动态流（含评论/收藏/转发/提及/举报） */
@Service
public class BoilingPointService {

    /** @提及 语法：@昵称 或 @用户名，昵称不含空格与 @ */
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([\\w\\u4e00-\\u9fa5-]+)");

    private final BoilingPointRepository boilingRepository;
    private final BoilingLikeRepository boilingLikeRepository;
    private final BoilingBookmarkRepository boilingBookmarkRepository;
    private final BoilingCommentRepository boilingCommentRepository;
    private final BoilingCommentVoteRepository boilingCommentVoteRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final UserFollowRepository followRepository;
    private final TopicFollowRepository topicFollowRepository;
    private final NotificationService notificationService;
    private final AnalyticsService analyticsService;
    private final Mapper mapper;

    public BoilingPointService(BoilingPointRepository boilingRepository,
                               BoilingLikeRepository boilingLikeRepository,
                               BoilingBookmarkRepository boilingBookmarkRepository,
                               BoilingCommentRepository boilingCommentRepository,
                               BoilingCommentVoteRepository boilingCommentVoteRepository,
                               ReportRepository reportRepository,
                               UserRepository userRepository,
                               UserFollowRepository followRepository,
                               TopicFollowRepository topicFollowRepository,
                               NotificationService notificationService,
                               AnalyticsService analyticsService,
                               Mapper mapper) {
        this.boilingRepository = boilingRepository;
        this.boilingLikeRepository = boilingLikeRepository;
        this.boilingBookmarkRepository = boilingBookmarkRepository;
        this.boilingCommentRepository = boilingCommentRepository;
        this.boilingCommentVoteRepository = boilingCommentVoteRepository;
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.topicFollowRepository = topicFollowRepository;
        this.notificationService = notificationService;
        this.analyticsService = analyticsService;
        this.mapper = mapper;
    }

    // ==================== 内容流 ====================

    @Transactional
    public BoilingDto create(Long userId, BoilingRequests.Create req) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
        author.setLastActiveAt(LocalDateTime.now());
        BoilingPoint b = new BoilingPoint();
        b.setAuthor(author);
        b.setContent(req.content.trim());
        b.setImageUrl(req.imageUrl == null ? null : req.imageUrl.trim());
        b.setCircle(req.circle == null || req.circle.isBlank() ? null : req.circle.trim());
        // 解析 @提及
        List<Long> mentioned = resolveMentions(req.content);
        if (!mentioned.isEmpty()) {
            b.setMentionedUserIds(mentioned.stream().map(String::valueOf).collect(Collectors.joining(",")));
        }
        BoilingPoint saved = boilingRepository.save(b);
        LevelSystem.award(author, LevelSystem.BOILING_CREATE);
        // 给被提及用户发通知
        User finalAuthor = author;
        mentioned.stream()
                .filter(id -> !id.equals(userId))
                .forEach(id -> userRepository.findById(id).ifPresent(u ->
                        notificationService.create(u, finalAuthor, NotificationType.MENTION, null)));
        analyticsService.track(userId, "boiling_create", "boiling", saved.getId(), null);
        return toDto(saved, false, false);
    }

    @Transactional(readOnly = true)
    public Page<BoilingDto> list(String feed, String circle, Long viewerId, Pageable pageable) {
        List<BoilingPoint> all = boilingRepository.findAll();
        List<BoilingPoint> sorted;
        String f = feed == null || feed.isBlank() ? "latest" : feed.toLowerCase();
        switch (f) {
            case "hot" -> sorted = all.stream()
                    .sorted(Comparator.comparingLong(BoilingPoint::getLikeCount).reversed()).toList();
            case "following" -> {
                if (viewerId == null) {
                    sorted = List.of();
                } else {
                    Set<Long> ids = followRepository.findByFollowerId(viewerId).stream()
                            .map(x -> x.getFollowee().getId()).collect(Collectors.toSet());
                    sorted = all.stream().filter(b -> ids.contains(b.getAuthor().getId()))
                            .sorted(Comparator.comparing(BoilingPoint::getCreatedAt).reversed()).toList();
                }
            }
            default -> sorted = all.stream()
                    .sorted(Comparator.comparing(BoilingPoint::getCreatedAt).reversed()).toList();
        }
        int start = (int) Math.min(pageable.getOffset(), sorted.size());
        int end = Math.min(start + pageable.getPageSize(), sorted.size());
        List<BoilingPoint> slice = sorted.subList(start, end);
        // 圈子过滤在分页前应用以保证 total 正确
        if (circle != null && !circle.isBlank()) {
            sorted = sorted.stream().filter(b -> circle.equals(b.getCircle())).toList();
            start = (int) Math.min(pageable.getOffset(), sorted.size());
            end = Math.min(start + pageable.getPageSize(), sorted.size());
            slice = sorted.subList(start, end);
        }
        List<BoilingDto> dtos = toDtos(slice, viewerId);
        return new PageImpl<>(dtos, pageable, sorted.size());
    }

    /** 批量转换，填充 liked/bookmarked 标志 */
    @Transactional(readOnly = true)
    public List<BoilingDto> toDtos(List<BoilingPoint> slice, Long viewerId) {
        List<Long> ids = slice.stream().map(BoilingPoint::getId).toList();
        Set<Long> liked = viewerId == null || slice.isEmpty() ? Set.of()
                : boilingLikeRepository.findLikedBoilingIds(viewerId, ids);
        Set<Long> marked = viewerId == null || slice.isEmpty() ? Set.of()
                : boilingBookmarkRepository.findBookmarkedBoilingIds(viewerId, ids);
        return slice.stream().map(b -> toDto(b, liked.contains(b.getId()), marked.contains(b.getId()))).toList();
    }

    /** 精选沸点（按赞+评论热度取前 N） */
    @Transactional(readOnly = true)
    public List<BoilingDto> featured(int limit, Long viewerId) {
        List<BoilingPoint> top = boilingRepository.findAll().stream()
                .sorted(Comparator.comparingLong((BoilingPoint b) ->
                        b.getLikeCount() * 2 + b.getCommentCount() * 3).reversed())
                .limit(limit).toList();
        return toDtos(top, viewerId);
    }

    /** 我的圈子 = 我关注的话题名（借用 TopicFollowRepository） */
    @Transactional(readOnly = true)
    public List<String> myCircles(Long userId) {
        if (userId == null) return List.of();
        return topicFollowRepository.findTopicNamesByUserId(userId);
    }

    // ==================== 互动 ====================

    @Transactional
    public Map<String, Boolean> toggleLike(Long userId, Long boilingId) {
        BoilingPoint b = boilingRepository.findById(boilingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "沸点不存在"));
        User u = userRepository.getReferenceById(userId);
        u.setLastActiveAt(LocalDateTime.now());
        Optional<BoilingLike> existing = boilingLikeRepository.findByUserIdAndBoilingPointId(userId, boilingId);
        if (existing.isPresent()) {
            boilingLikeRepository.delete(existing.get());
            b.setLikeCount(Math.max(0, b.getLikeCount() - 1));
            boilingRepository.save(b);
            return Map.of("liked", false);
        }
        BoilingLike bl = new BoilingLike();
        bl.setUser(u);
        bl.setBoilingPoint(b);
        boilingLikeRepository.save(bl);
        b.setLikeCount(b.getLikeCount() + 1);
        boilingRepository.save(b);
        notificationService.create(b.getAuthor(), u, NotificationType.BOILING_LIKE, null);
        analyticsService.track(userId, "boiling_like", "boiling", boilingId, null);
        return Map.of("liked", true);
    }

    @Transactional
    public Map<String, Boolean> toggleBookmark(Long userId, Long boilingId) {
        BoilingPoint b = boilingRepository.findById(boilingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "沸点不存在"));
        User u = userRepository.getReferenceById(userId);
        Optional<BoilingBookmark> existing = boilingBookmarkRepository.findByUserIdAndBoilingPointId(userId, boilingId);
        if (existing.isPresent()) {
            boilingBookmarkRepository.delete(existing.get());
            b.setBookmarkCount(Math.max(0, b.getBookmarkCount() - 1));
            boilingRepository.save(b);
            return Map.of("bookmarked", false);
        }
        BoilingBookmark bm = new BoilingBookmark();
        bm.setUser(u);
        bm.setBoilingPoint(b);
        boilingBookmarkRepository.save(bm);
        b.setBookmarkCount(b.getBookmarkCount() + 1);
        boilingRepository.save(b);
        analyticsService.track(userId, "boiling_bookmark", "boiling", boilingId, null);
        return Map.of("bookmarked", true);
    }

    /** 转发：分享数+1 并埋点（返回可用于前端展示的新计数） */
    @Transactional
    public Map<String, Long> share(Long userId, Long boilingId) {
        BoilingPoint b = boilingRepository.findById(boilingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "沸点不存在"));
        b.setShareCount(b.getShareCount() + 1);
        boilingRepository.save(b);
        analyticsService.track(userId, "boiling_share", "boiling", boilingId, null);
        return Map.of("shareCount", b.getShareCount());
    }

    @Transactional
    public void delete(Long id, Long userId) {
        BoilingPoint b = boilingRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "沸点不存在"));
        if (!b.getAuthor().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "只有作者可以删除");
        }
        // 先删评论投票（评论删除后变孤儿），再删评论及其它关联
        boilingCommentRepository.findByBoilingPointId(id)
                .forEach(c -> boilingCommentVoteRepository.deleteByCommentId(c.getId()));
        boilingCommentRepository.deleteByBoilingPointId(id);
        boilingLikeRepository.deleteByBoilingPointId(id);
        boilingBookmarkRepository.deleteByBoilingPointId(id);
        boilingRepository.delete(b);
    }

    // ==================== 评论 ====================

    @Transactional
    public BoilingCommentDto addComment(Long boilingId, Long userId, BoilingRequests.Comment req) {
        BoilingPoint b = boilingRepository.findById(boilingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "沸点不存在"));
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
        author.setLastActiveAt(LocalDateTime.now());
        BoilingComment c = new BoilingComment();
        c.setBoilingPoint(b);
        c.setAuthor(author);
        c.setContent(req.content.trim());
        if (req.parentId != null) {
            BoilingComment parent = boilingCommentRepository.findById(req.parentId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "父评论不存在"));
            if (!parent.getBoilingPoint().getId().equals(boilingId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "父评论不属于该沸点");
            }
            c.setParentId(parent.getParentId() != null ? parent.getParentId() : parent.getId());
            c.setReplyToUser(parent.getAuthor());
            if (!parent.getAuthor().getId().equals(b.getAuthor().getId())) {
                notificationService.create(parent.getAuthor(), author, NotificationType.BOILING_COMMENT, null);
            }
        }
        // 解析评论中的 @提及
        List<Long> mentioned = resolveMentions(req.content);
        if (!mentioned.isEmpty()) {
            c.setMentionedUserIds(mentioned.stream().map(String::valueOf).collect(Collectors.joining(",")));
        }
        BoilingComment saved = boilingCommentRepository.save(c);
        notificationService.create(b.getAuthor(), author, NotificationType.BOILING_COMMENT, null);
        // 通知被提及者
        User finalAuthor = author;
        mentioned.stream()
                .filter(id -> !id.equals(userId))
                .forEach(id -> userRepository.findById(id).ifPresent(u ->
                        notificationService.create(u, finalAuthor, NotificationType.MENTION, null)));
        b.setCommentCount(b.getCommentCount() + 1);
        boilingRepository.save(b);
        LevelSystem.award(author, LevelSystem.COMMENT_CREATE);
        analyticsService.track(userId, "boiling_comment", "boiling", boilingId, null);
        return toCommentDto(saved, null);
    }

    /**
     * 评论列表：顶层分页 + 嵌套回复全量
     * @param sort default=默认(时间正序) latest=最新在前 hot=最热(赞-踩)在前
     */
    @Transactional(readOnly = true)
    public Page<BoilingCommentDto> comments(Long boilingId, String sort, Long viewerId, Pageable pageable) {
        boilingRepository.findById(boilingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "沸点不存在"));
        List<BoilingComment> tops = boilingCommentRepository.findByBoilingPointId(boilingId).stream()
                .filter(c -> c.getParentId() == null).toList();
        Comparator<BoilingComment> cmp = switch (sort == null ? "default" : sort.toLowerCase()) {
            case "latest" -> Comparator.comparing(BoilingComment::getCreatedAt).reversed();
            case "hot" -> Comparator.comparingLong((BoilingComment c) ->
                    c.getLikeCount() - c.getDislikeCount()).reversed()
                    .thenComparing(BoilingComment::getCreatedAt).reversed();
            default -> Comparator.comparing(BoilingComment::getCreatedAt);
        };
        List<BoilingComment> sortedTops = tops.stream().sorted(cmp).toList();
        int start = (int) Math.min(pageable.getOffset(), sortedTops.size());
        int end = Math.min(start + pageable.getPageSize(), sortedTops.size());
        List<BoilingComment> pageItems = sortedTops.subList(start, end);
        List<Long> topIds = pageItems.stream().map(BoilingComment::getId).toList();
        Map<Long, List<BoilingComment>> repliesMap = topIds.isEmpty() ? Map.of()
                : boilingCommentRepository.findByParentIdIn(topIds).stream()
                        .sorted(Comparator.comparing(BoilingComment::getCreatedAt))
                        .collect(Collectors.groupingBy(BoilingComment::getParentId));

        // 当前用户对这些评论的投票
        List<Long> allIds = new ArrayList<>(topIds);
        repliesMap.values().forEach(rs -> rs.forEach(r -> allIds.add(r.getId())));
        Map<Long, Boolean> myVotes = new HashMap<>();
        if (viewerId != null && !allIds.isEmpty()) {
            for (BoilingCommentVote v : boilingCommentVoteRepository.findByUserIdAndCommentIdIn(viewerId, allIds)) {
                myVotes.put(v.getComment().getId(), v.isUp());
            }
        }

        List<BoilingCommentDto> dtos = pageItems.stream()
                .map(c -> toCommentDto(c, myVotes.get(c.getId()), repliesMap.getOrDefault(c.getId(), List.of()), myVotes))
                .toList();
        return new PageImpl<>(dtos, pageable, sortedTops.size());
    }

    @Transactional
    public Map<String, Object> voteComment(Long userId, Long commentId, boolean up) {
        BoilingComment c = boilingCommentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "评论不存在"));
        Optional<BoilingCommentVote> existing = boilingCommentVoteRepository.findByUserIdAndCommentId(userId, commentId);
        if (existing.isPresent()) {
            BoilingCommentVote v = existing.get();
            if (v.isUp() == up) {
                // 再点一次取消
                boilingCommentVoteRepository.delete(v);
                if (up) c.setLikeCount(Math.max(0, c.getLikeCount() - 1));
                else c.setDislikeCount(Math.max(0, c.getDislikeCount() - 1));
                boilingCommentRepository.save(c);
                Map<String, Object> r = new HashMap<>();
                r.put("myVote", null);
                r.put("likeCount", c.getLikeCount());
                r.put("dislikeCount", c.getDislikeCount());
                return r;
            }
            // 切换方向
            if (up) {
                c.setLikeCount(c.getLikeCount() + 1);
                c.setDislikeCount(Math.max(0, c.getDislikeCount() - 1));
            } else {
                c.setDislikeCount(c.getDislikeCount() + 1);
                c.setLikeCount(Math.max(0, c.getLikeCount() - 1));
            }
            v.setUp(up);
            boilingCommentVoteRepository.save(v);
            boilingCommentRepository.save(c);
            return Map.of("myVote", up, "likeCount", c.getLikeCount(), "dislikeCount", c.getDislikeCount());
        }
        BoilingCommentVote v = new BoilingCommentVote();
        v.setUser(userRepository.getReferenceById(userId));
        v.setComment(c);
        v.setUp(up);
        boilingCommentVoteRepository.save(v);
        if (up) {
            c.setLikeCount(c.getLikeCount() + 1);
            if (!c.getAuthor().getId().equals(userId)) {
                LevelSystem.award(c.getAuthor(), 1);
            }
        } else {
            c.setDislikeCount(c.getDislikeCount() + 1);
        }
        boilingCommentRepository.save(c);
        analyticsService.track(userId, up ? "comment_like" : "comment_dislike", "boiling_comment", commentId, null);
        return Map.of("myVote", up, "likeCount", c.getLikeCount(), "dislikeCount", c.getDislikeCount());
    }

    @Transactional
    public Map<String, Object> report(Long userId, String targetType, Long targetId, BoilingRequests.Report req) {
        Report.ReportTargetType type;
        try {
            type = Report.ReportTargetType.valueOf(targetType.toUpperCase());
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "targetType 必须是 BOILING 或 BOILING_COMMENT");
        }
        if (type == Report.ReportTargetType.BOILING
                && boilingRepository.findById(targetId).isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "沸点不存在");
        }
        if (type == Report.ReportTargetType.BOILING_COMMENT
                && boilingCommentRepository.findById(targetId).isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "评论不存在");
        }
        Report r = new Report();
        r.setReporter(userRepository.getReferenceById(userId));
        r.setTargetType(type);
        r.setTargetId(targetId);
        r.setReason(req.reason.trim());
        reportRepository.save(r);
        // 同步在被举报对象上打标（前端可不展示已举报内容）
        if (type == Report.ReportTargetType.BOILING_COMMENT) {
            boilingCommentRepository.findById(targetId).ifPresent(c -> {
                c.setReported(true);
                boilingCommentRepository.save(c);
            });
        }
        analyticsService.track(userId, "report", targetType.toLowerCase(), targetId, req.reason);
        return Map.of("reported", true);
    }

    // ==================== helpers ====================

    /** 解析 @提及：匹配 @昵称/@用户名 → 已存在用户 id 列表 */
    private List<Long> resolveMentions(String content) {
        if (content == null || content.isBlank()) return List.of();
        Set<Long> ids = new LinkedHashSet<>();
        Matcher m = MENTION_PATTERN.matcher(content);
        while (m.find()) {
            String name = m.group(1);
            userRepository.findByUsername(name).ifPresent(u -> ids.add(u.getId()));
            if (ids.size() >= 10) break;
        }
        return new ArrayList<>(ids);
    }

    private BoilingDto toDto(BoilingPoint b, boolean liked, boolean bookmarked) {
        BoilingDto d = new BoilingDto();
        d.setId(b.getId());
        d.setContent(b.getContent());
        d.setImageUrl(b.getImageUrl());
        d.setCircle(b.getCircle());
        d.setLikeCount(b.getLikeCount());
        d.setCommentCount(b.getCommentCount());
        d.setBookmarkCount(b.getBookmarkCount());
        d.setShareCount(b.getShareCount());
        d.setLiked(liked);
        d.setBookmarked(bookmarked);
        d.setAuthor(mapper.toUserSummary(b.getAuthor()));
        d.setCreatedAt(b.getCreatedAt());
        return d;
    }

    private BoilingCommentDto toCommentDto(BoilingComment c, Boolean myVote) {
        return toCommentDto(c, myVote, List.of(), Map.of());
    }

    private BoilingCommentDto toCommentDto(BoilingComment c, Boolean myVote,
                                           List<BoilingComment> replies, Map<Long, Boolean> myVotes) {
        BoilingCommentDto d = new BoilingCommentDto();
        d.setId(c.getId());
        d.setBoilingId(c.getBoilingPoint().getId());
        d.setContent(c.getContent());
        d.setParentId(c.getParentId());
        d.setReplyToUser(c.getReplyToUser() == null ? null : mapper.toUserSummary(c.getReplyToUser()));
        d.setAuthor(mapper.toUserSummary(c.getAuthor()));
        d.setLikeCount(c.getLikeCount());
        d.setDislikeCount(c.getDislikeCount());
        d.setMyVote(myVote);
        d.setCreatedAt(c.getCreatedAt());
        d.setReplies(replies.stream()
                .map(r -> toCommentDto(r, myVotes.get(r.getId())))
                .collect(Collectors.toCollection(ArrayList::new)));
        return d;
    }
}