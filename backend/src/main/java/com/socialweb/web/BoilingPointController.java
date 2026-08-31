package com.socialweb.web;

import com.socialweb.dto.BoilingCommentDto;
import com.socialweb.dto.BoilingDto;
import com.socialweb.dto.BoilingRequests;
import com.socialweb.service.AnalyticsService;
import com.socialweb.service.BoilingPointService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/boiling")
public class BoilingPointController {

    private final BoilingPointService boilingService;
    private final AnalyticsService analyticsService;

    public BoilingPointController(BoilingPointService boilingService,
                                  AnalyticsService analyticsService) {
        this.boilingService = boilingService;
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public Page<BoilingDto> list(@RequestParam(required = false) String feed,
                                 @RequestParam(required = false) String circle,
                                 @PageableDefault(size = 20) Pageable pageable) {
        return boilingService.list(feed, circle, AuthHelper.optionalUser(), pageable);
    }

    /** 精选沸点（右侧栏） */
    @GetMapping("/featured")
    public List<BoilingDto> featured(@RequestParam(defaultValue = "5") int limit) {
        return boilingService.featured(Math.min(Math.max(limit, 1), 20), AuthHelper.optionalUser());
    }

    /** 我的圈子（我关注的话题名） */
    @GetMapping("/circles")
    public List<String> circles() {
        return boilingService.myCircles(AuthHelper.requireUser());
    }

    @PostMapping
    public BoilingDto create(@Valid @RequestBody BoilingRequests.Create req) {
        return boilingService.create(AuthHelper.requireUser(), req);
    }

    @PostMapping("/{id}/like")
    public Map<String, Boolean> like(@PathVariable Long id) {
        return boilingService.toggleLike(AuthHelper.requireUser(), id);
    }

    @PostMapping("/{id}/bookmark")
    public Map<String, Boolean> bookmark(@PathVariable Long id) {
        return boilingService.toggleBookmark(AuthHelper.requireUser(), id);
    }

    /** 转发/分享计数 */
    @PostMapping("/{id}/share")
    public Map<String, Long> share(@PathVariable Long id) {
        return boilingService.share(AuthHelper.requireUser(), id);
    }

    // ==================== 评论 ====================

    @GetMapping("/{id}/comments")
    public Page<BoilingCommentDto> comments(@PathVariable Long id,
                                            @RequestParam(required = false) String sort,
                                            @PageableDefault(size = 20) Pageable pageable) {
        return boilingService.comments(id, sort, AuthHelper.optionalUser(), pageable);
    }

    @PostMapping("/{id}/comments")
    public BoilingCommentDto addComment(@PathVariable Long id,
                                        @Valid @RequestBody BoilingRequests.Comment req) {
        return boilingService.addComment(id, AuthHelper.requireUser(), req);
    }

    /** 评论点赞/点踩（body: {"up": true|false}） */
    @PostMapping("/comments/{commentId}/vote")
    public Map<String, Object> voteComment(@PathVariable Long commentId,
                                           @RequestBody Map<String, Boolean> body) {
        boolean up = !Boolean.FALSE.equals(body.get("up"));
        return boilingService.voteComment(AuthHelper.requireUser(), commentId, up);
    }

    /** 举报（body: {"reason": "..."}） */
    @PostMapping("/{targetType}/{targetId}/report")
    public Map<String, Object> report(@PathVariable String targetType,
                                      @PathVariable Long targetId,
                                      @Valid @RequestBody BoilingRequests.Report req) {
        return boilingService.report(AuthHelper.requireUser(), targetType, targetId, req);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        boilingService.delete(id, AuthHelper.requireUser());
        return Map.of("message", "已删除");
    }

    // ==================== 埋点 ====================

    /** 前端行为埋点上报（无需登录，匿名事件 userId 为空） */
    @PostMapping("/analytics")
    public Map<String, Object> track(@Valid @RequestBody BoilingRequests.Analytics req) {
        analyticsService.track(AuthHelper.optionalUser(), req.action, req.targetType, req.targetId, req.extra);
        return Map.of("ok", true);
    }

    /** 埋点统计汇总（数据指标） */
    @GetMapping("/analytics/summary")
    public Map<String, Long> analyticsSummary() {
        return analyticsService.summary();
    }
}