package com.socialweb.web;

import com.socialweb.dto.PostSummary;
import com.socialweb.dto.TagCount;
import com.socialweb.service.PostService;
import com.socialweb.service.TagService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final PostService postService;
    private final TagService tagService;

    public SearchController(PostService postService, TagService tagService) {
        this.postService = postService;
        this.tagService = tagService;
    }

    @GetMapping("/search")
    public Page<PostSummary> search(@RequestParam(required = false) String q,
                                    @PageableDefault(size = 20) Pageable pageable) {
        return postService.search(q, AuthHelper.optionalUser(), pageable);
    }

    @GetMapping("/tags")
    public List<TagCount> tags(@RequestParam(defaultValue = "20") int limit) {
        return tagService.popular(Math.min(Math.max(limit, 1), 100));
    }
}
