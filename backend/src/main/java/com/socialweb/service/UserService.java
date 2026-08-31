package com.socialweb.service;

import com.socialweb.dto.PostSummary;
import com.socialweb.dto.ProfileRequests;
import com.socialweb.dto.UserDto;
import com.socialweb.dto.UserSummary;
import com.socialweb.entity.NotificationType;
import com.socialweb.entity.Post;
import com.socialweb.entity.PostBookmark;
import com.socialweb.entity.User;
import com.socialweb.entity.UserFollow;
import com.socialweb.repository.PostBookmarkRepository;
import com.socialweb.repository.PostRepository;
import com.socialweb.repository.UserFollowRepository;
import com.socialweb.repository.UserRepository;
import com.socialweb.web.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserFollowRepository followRepository;
    private final PostBookmarkRepository bookmarkRepository;
    private final NotificationService notificationService;
    private final Mapper mapper;

    public UserService(UserRepository userRepository,
                       UserFollowRepository followRepository,
                       PostBookmarkRepository bookmarkRepository,
                       NotificationService notificationService,
                       Mapper mapper) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.notificationService = notificationService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public UserDto get(Long id, Long viewerId) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
        return mapper.toUserDto(u, viewerId);
    }

    @Transactional
    public UserDto updateProfile(Long userId, ProfileRequests.Update req) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
        if (req.nickname != null && !req.nickname.isBlank()) u.setNickname(req.nickname.trim());
        if (req.avatar != null) u.setAvatar(req.avatar.trim());
        if (req.bio != null) u.setBio(req.bio.trim());
        return mapper.toUserDto(u, userId);
    }

    /** 切换关注，返回 {followed: boolean} */
    @Transactional
    public Map<String, Boolean> toggleFollow(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "不能关注自己");
        }
        User followee = userRepository.findById(followeeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
        User follower = userRepository.getReferenceById(followerId);
        boolean exists = followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId);
        if (exists) {
            followRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId);
            return Map.of("followed", false);
        }
        UserFollow f = new UserFollow();
        f.setFollower(follower);
        f.setFollowee(followee);
        followRepository.save(f);
        LevelSystem.award(followee, LevelSystem.RECEIVE_FOLLOW);
        notificationService.create(followee, follower, NotificationType.FOLLOW, null);
        return Map.of("followed", true);
    }

    @Transactional(readOnly = true)
    public Page<UserSummary> following(Long userId, Pageable pageable) {
        return followRepository.findByFollowerId(userId, pageable)
                .map(uf -> mapper.toUserSummary(uf.getFollowee()));
    }

    @Transactional(readOnly = true)
    public Page<UserSummary> followers(Long userId, Pageable pageable) {
        return followRepository.findByFolloweeId(userId, pageable)
                .map(uf -> mapper.toUserSummary(uf.getFollower()));
    }

    @Transactional(readOnly = true)
    public Page<PostSummary> bookmarks(Long userId, Pageable pageable) {
        Page<Post> page = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(PostBookmark::getPost);
        List<Post> posts = page.getContent();
        List<PostSummary> summaries = mapper.toSummaries(posts, userId);
        return new org.springframework.data.domain.PageImpl<>(summaries, pageable, page.getTotalElements());
    }
}
