package com.socialweb.web;

import com.socialweb.dto.BoilingDto;
import com.socialweb.dto.BoilingRequests;
import com.socialweb.service.BoilingPointService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/boiling")
public class BoilingPointController {

    private final BoilingPointService boilingService;

    public BoilingPointController(BoilingPointService boilingService) {
        this.boilingService = boilingService;
    }

    @GetMapping
    public Page<BoilingDto> list(@RequestParam(required = false) String feed,
                                 @PageableDefault(size = 20) Pageable pageable) {
        return boilingService.list(feed, AuthHelper.optionalUser(), pageable);
    }

    @PostMapping
    public BoilingDto create(@Valid @RequestBody BoilingRequests.Create req) {
        return boilingService.create(AuthHelper.requireUser(), req);
    }

    @PostMapping("/{id}/like")
    public Map<String, Boolean> like(@PathVariable Long id) {
        return boilingService.toggleLike(AuthHelper.requireUser(), id);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        boilingService.delete(id, AuthHelper.requireUser());
        return Map.of("message", "已删除");
    }
}