package com.socialweb.service;

import com.socialweb.dto.TagCount;
import com.socialweb.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TagService {

    private final PostRepository postRepository;

    public TagService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional(readOnly = true)
    public List<TagCount> popular(int limit) {
        Map<String, Long> counts = new HashMap<>();
        postRepository.findAll().forEach(p ->
                Mapper.parseTags(p.getTags()).forEach(t -> counts.merge(t, 1L, Long::sum)));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> new TagCount(e.getKey(), e.getValue()))
                .toList();
    }
}
