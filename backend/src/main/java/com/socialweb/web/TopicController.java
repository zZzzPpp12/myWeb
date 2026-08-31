package com.socialweb.web;

import com.socialweb.dto.PostSummary;
import com.socialweb.dto.TopicDto;
import com.socialweb.service.TopicService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping
    public List<TopicDto> list(@RequestParam(defaultValue = "20") int limit) {
        return topicService.popular(Math.min(Math.max(limit, 1), 100), AuthHelper.optionalUser());
    }

    @GetMapping("/{name}")
    public TopicDto get(@PathVariable String name) {
        return topicService.get(name, AuthHelper.optionalUser());
    }

    @GetMapping("/{name}/posts")
    public Page<PostSummary> posts(@PathVariable String name,
                                   @PageableDefault(size = 20) Pageable pageable) {
        return topicService.posts(name, AuthHelper.optionalUser(), pageable);
    }

    @PostMapping("/{name}/follow")
    public Map<String, Boolean> follow(@PathVariable String name) {
        return topicService.toggleFollow(name, AuthHelper.requireUser());
    }
}