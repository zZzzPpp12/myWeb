package com.socialweb.service;

import com.socialweb.dto.PostSummary;
import com.socialweb.dto.TopicDto;
import com.socialweb.entity.Topic;
import com.socialweb.entity.TopicFollow;
import com.socialweb.repository.PostRepository;
import com.socialweb.repository.TopicFollowRepository;
import com.socialweb.repository.TopicRepository;
import com.socialweb.repository.UserRepository;
import com.socialweb.web.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 话题体系（知乎「话题」+ 掘金「标签」融合） */
@Service
public class TopicService {

    private final TopicRepository topicRepository;
    private final TopicFollowRepository topicFollowRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostService postService;
    private final Mapper mapper;

    public TopicService(TopicRepository topicRepository,
                        TopicFollowRepository topicFollowRepository,
                        UserRepository userRepository,
                        PostRepository postRepository,
                        PostService postService,
                        Mapper mapper) {
        this.topicRepository = topicRepository;
        this.topicFollowRepository = topicFollowRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.postService = postService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<TopicDto> popular(int limit, Long viewerId) {
        Map<String, Long> counts = postCountByTag();
        return topicRepository.findAll().stream()
                .sorted(Comparator.comparingLong((Topic t) -> -t.getFollowerCount())
                        .thenComparingLong(t -> -counts.getOrDefault(t.getName(), 0L)))
                .limit(limit)
                .map(t -> toDto(t, counts.getOrDefault(t.getName(), 0L), viewerId))
                .toList();
    }

    @Transactional(readOnly = true)
    public TopicDto get(String name, Long viewerId) {
        Topic t = topicRepository.findByName(name)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "话题不存在"));
        return toDto(t, postCountByTag().getOrDefault(name, 0L), viewerId);
    }

    @Transactional(readOnly = true)
    public Page<PostSummary> posts(String name, Long viewerId, Pageable pageable) {
        topicRepository.findByName(name)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "话题不存在"));
        return postService.list(null, name, null, viewerId, pageable);
    }

    @Transactional
    public Map<String, Boolean> toggleFollow(String name, Long userId) {
        Topic t = topicRepository.findByName(name)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "话题不存在"));
        Optional<TopicFollow> existing = topicFollowRepository.findByUserIdAndTopicId(userId, t.getId());
        if (existing.isPresent()) {
            topicFollowRepository.delete(existing.get());
            t.setFollowerCount(Math.max(0, t.getFollowerCount() - 1));
            topicRepository.save(t);
            return Map.of("followed", false);
        }
        TopicFollow tf = new TopicFollow();
        tf.setUser(userRepository.getReferenceById(userId));
        tf.setTopic(t);
        topicFollowRepository.save(tf);
        t.setFollowerCount(t.getFollowerCount() + 1);
        topicRepository.save(t);
        return Map.of("followed", true);
    }

    private Map<String, Long> postCountByTag() {
        Map<String, Long> counts = new HashMap<>();
        postRepository.findAll().forEach(p ->
                Mapper.parseTags(p.getTags()).forEach(t -> counts.merge(t, 1L, Long::sum)));
        return counts;
    }

    private TopicDto toDto(Topic t, long postCount, Long viewerId) {
        TopicDto d = new TopicDto();
        d.setId(t.getId());
        d.setName(t.getName());
        d.setDescription(t.getDescription());
        d.setEmoji(t.getEmoji());
        d.setFollowerCount(t.getFollowerCount());
        d.setPostCount(postCount);
        d.setFollowed(viewerId != null && topicFollowRepository.existsByUserIdAndTopicId(viewerId, t.getId()));
        return d;
    }
}