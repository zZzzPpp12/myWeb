package com.socialweb.web;

import com.socialweb.dto.CommentDto;
import com.socialweb.dto.PostDetail;
import com.socialweb.dto.PostRequests;
import com.socialweb.dto.PostSummary;
import com.socialweb.service.CommentService;
import com.socialweb.service.PostService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final CommentService commentService;

    public PostController(PostService postService, CommentService commentService) {
        this.postService = postService;
        this.commentService = commentService;
    }

    @PostMapping
    public PostDetail create(@Valid @RequestBody PostRequests.Create req) {
        return postService.create(AuthHelper.requireUser(), req);
    }

    @GetMapping
    public Page<PostSummary> list(@RequestParam(required = false) String feed,
                                  @RequestParam(required = false) String tag,
                                  @RequestParam(required = false) Long author,
                                  @PageableDefault(size = 20) Pageable pageable) {
        return postService.list(feed, tag, author, AuthHelper.optionalUser(), pageable);
    }

    @GetMapping("/recommended")
    public List<PostSummary> recommended(@RequestParam(defaultValue = "10") int limit) {
        return postService.recommended(AuthHelper.optionalUser(), Math.min(Math.max(limit, 1), 50));
    }

    @GetMapping("/{id}")
    public PostDetail detail(@PathVariable Long id) {
        return postService.detail(id, AuthHelper.optionalUser());
    }

    @PutMapping("/{id}")
    public PostDetail update(@PathVariable Long id, @Valid @RequestBody PostRequests.Update req) {
        return postService.update(id, AuthHelper.requireUser(), req);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        postService.delete(id, AuthHelper.requireUser());
        return Map.of("message", "已删除");
    }

    @PostMapping("/{id}/comments")
    public CommentDto addComment(@PathVariable Long id, @Valid @RequestBody PostRequests.Comment req) {
        return commentService.add(id, AuthHelper.requireUser(), req);
    }

    @GetMapping("/{id}/comments")
    public Page<CommentDto> comments(@PathVariable Long id,
                                     @PageableDefault(size = 20) Pageable pageable) {
        return commentService.list(id, pageable);
    }

    @PostMapping("/{id}/like")
    public Map<String, Boolean> like(@PathVariable Long id) {
        return postService.toggleLike(AuthHelper.requireUser(), id);
    }

    @PostMapping("/{id}/dislike")
    public Map<String, Boolean> dislike(@PathVariable Long id) {
        return postService.toggleDislike(AuthHelper.requireUser(), id);
    }

    @PostMapping("/{id}/bookmark")
    public Map<String, Boolean> bookmark(@PathVariable Long id) {
        return postService.toggleBookmark(AuthHelper.requireUser(), id);
    }
}
