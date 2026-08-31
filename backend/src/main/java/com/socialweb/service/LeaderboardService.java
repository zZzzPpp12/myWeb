package com.socialweb.service;

import com.socialweb.dto.PostSummary;
import com.socialweb.dto.UserSummary;
import com.socialweb.entity.Post;
import com.socialweb.entity.User;
import com.socialweb.repository.PostRepository;
import com.socialweb.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** 排行榜：热文榜 + 创作者榜（掘金式榜单） */
@Service
public class LeaderboardService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final Mapper mapper;

    public LeaderboardService(PostRepository postRepository,
                              UserRepository userRepository,
                              Mapper mapper) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PostSummary> posts(int limit, Long viewerId) {
        LocalDateTime now = LocalDateTime.now();
        return mapper.toSummaries(postRepository.findAll().stream()
                .sorted(Comparator.comparingDouble((Post p) -> PostService.hotScore(p, now)).reversed())
                .limit(limit).toList(), viewerId);
    }

    @Transactional(readOnly = true)
    public List<UserSummary> users(int limit) {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparingInt(User::getReputation).reversed())
                .limit(limit)
                .map(mapper::toUserSummary).toList();
    }
}