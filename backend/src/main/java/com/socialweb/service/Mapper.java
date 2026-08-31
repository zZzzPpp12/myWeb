package com.socialweb.service;

import com.socialweb.dto.PostDetail;
import com.socialweb.dto.PostSummary;
import com.socialweb.dto.UserDto;
import com.socialweb.dto.UserSummary;
import com.socialweb.entity.Post;
import com.socialweb.entity.User;
import com.socialweb.repository.PostBookmarkRepository;
import com.socialweb.repository.PostDislikeRepository;
import com.socialweb.repository.PostLikeRepository;
import com.socialweb.repository.PostRepository;
import com.socialweb.repository.UserFollowRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** 实体 -> DTO 统一映射（需在事务内调用以读取懒加载字段） */
@Component
public class Mapper {

    private final UserFollowRepository followRepository;
    private final PostLikeRepository likeRepository;
    private final PostDislikeRepository dislikeRepository;
    private final PostBookmarkRepository bookmarkRepository;
    private final PostRepository postRepository;
    private final com.socialweb.repository.BoilingPointRepository boilingRepository;

    public Mapper(UserFollowRepository followRepository,
                  PostLikeRepository likeRepository,
                  PostDislikeRepository dislikeRepository,
                  PostBookmarkRepository bookmarkRepository,
                  PostRepository postRepository,
                  com.socialweb.repository.BoilingPointRepository boilingRepository) {
        this.followRepository = followRepository;
        this.likeRepository = likeRepository;
        this.dislikeRepository = dislikeRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.postRepository = postRepository;
        this.boilingRepository = boilingRepository;
    }

    public static List<String> parseTags(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /** 5 分钟内活跃视为在线 */
    private static final java.time.Duration ONLINE_WINDOW = java.time.Duration.ofMinutes(5);

    public UserSummary toUserSummary(User u) {
        if (u == null) return null;
        UserSummary s = new UserSummary();
        s.setId(u.getId());
        s.setUsername(u.getUsername());
        s.setNickname(u.getNickname());
        s.setAvatar(u.getAvatar());
        int rep = u.getReputation();
        s.setReputation(rep);
        s.setLevel(LevelSystem.level(rep));
        s.setLevelName(LevelSystem.levelName(rep));
        s.setOnline(u.getLastActiveAt() != null
                && u.getLastActiveAt().isAfter(LocalDateTime.now().minus(ONLINE_WINDOW)));
        return s;
    }

    public UserDto toUserDto(User u, Long viewerId) {
        UserDto dto = new UserDto();
        dto.setId(u.getId());
        dto.setUsername(u.getUsername());
        dto.setNickname(u.getNickname());
        dto.setAvatar(u.getAvatar());
        dto.setBio(u.getBio());
        dto.setRole(u.getRole().name());
        dto.setFollowersCount(followRepository.countByFolloweeId(u.getId()));
        dto.setFollowingCount(followRepository.countByFollowerId(u.getId()));
        dto.setPostsCount(postRepository.countByAuthorId(u.getId()));
        dto.setFollowed(viewerId != null && !viewerId.equals(u.getId())
                && followRepository.existsByFollowerIdAndFolloweeId(viewerId, u.getId()));
        int rep = u.getReputation();
        dto.setReputation(rep);
        dto.setLevel(LevelSystem.level(rep));
        dto.setLevelName(LevelSystem.levelName(rep));
        dto.setOnline(u.getLastActiveAt() != null
                && u.getLastActiveAt().isAfter(LocalDateTime.now().minus(ONLINE_WINDOW)));
        // 互动统计：收到的赞（文章赞）+ 沸点数
        dto.setLikesReceived(postRepository.sumLikeCountByAuthorId(u.getId()));
        dto.setBoilingsCount(boilingRepository.countByAuthorId(u.getId()));
        dto.setLastActiveAt(u.getLastActiveAt());
        dto.setCreatedAt(u.getCreatedAt());
        return dto;
    }

    public PostSummary toPostSummary(Post p, boolean liked, boolean bookmarked) {
        return toPostSummary(p, liked, bookmarked, false);
    }

    public PostSummary toPostSummary(Post p, boolean liked, boolean bookmarked, boolean downvoted) {
        PostSummary s = new PostSummary();
        fill(p, s, liked, bookmarked, downvoted);
        return s;
    }

    public PostDetail toPostDetail(Post p, boolean liked, boolean bookmarked, Boolean followed) {
        return toPostDetail(p, liked, bookmarked, false, followed);
    }

    public PostDetail toPostDetail(Post p, boolean liked, boolean bookmarked, boolean downvoted, Boolean followed) {
        PostDetail d = new PostDetail();
        fill(p, d, liked, bookmarked, downvoted);
        d.setContent(p.getContent());
        d.setFollowed(followed);
        return d;
    }

    private void fill(Post p, PostSummary s, boolean liked, boolean bookmarked, boolean downvoted) {
        s.setId(p.getId());
        s.setTitle(p.getTitle());
        String c = p.getContent() == null ? "" : p.getContent();
        s.setExcerpt(c.length() > 120 ? c.substring(0, 120) : c);
        s.setType(p.getType().name());
        s.setTags(parseTags(p.getTags()));
        s.setAuthor(toUserSummary(p.getAuthor()));
        s.setViewCount(p.getViewCount());
        s.setLikeCount(p.getLikeCount());
        s.setDislikeCount(p.getDislikeCount());
        s.setScore(p.getLikeCount() - p.getDislikeCount());
        s.setCommentCount(p.getCommentCount());
        s.setBookmarkCount(p.getBookmarkCount());
        s.setLiked(liked);
        s.setDownvoted(downvoted);
        s.setBookmarked(bookmarked);
        s.setCreatedAt(p.getCreatedAt());
        s.setUpdatedAt(p.getUpdatedAt());
    }

    /** 批量转换，按当前登录用户填充 liked/downvoted/bookmarked 标志 */
    public List<PostSummary> toSummaries(List<Post> posts, Long uid) {
        List<Long> ids = posts.stream().map(Post::getId).toList();
        Set<Long> liked = uid == null || posts.isEmpty() ? Set.of()
                : likeRepository.findLikedPostIds(uid, ids);
        Set<Long> disliked = uid == null || posts.isEmpty() ? Set.of()
                : dislikeRepository.findDislikedPostIds(uid, ids);
        Set<Long> marked = uid == null || posts.isEmpty() ? Set.of()
                : bookmarkRepository.findBookmarkedPostIds(uid, ids);
        return posts.stream()
                .map(p -> toPostSummary(p, liked.contains(p.getId()), marked.contains(p.getId()), disliked.contains(p.getId())))
                .toList();
    }

    public boolean liked(Long uid, Long postId) {
        return uid != null && likeRepository.existsByUserIdAndPostId(uid, postId);
    }

    public boolean downvoted(Long uid, Long postId) {
        return uid != null && dislikeRepository.existsByUserIdAndPostId(uid, postId);
    }

    public boolean bookmarked(Long uid, Long postId) {
        return uid != null && bookmarkRepository.existsByUserIdAndPostId(uid, postId);
    }

    public boolean follows(Long followerId, Long followeeId) {
        return followerId != null && followeeId != null && !followerId.equals(followeeId)
                && followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId);
    }
}