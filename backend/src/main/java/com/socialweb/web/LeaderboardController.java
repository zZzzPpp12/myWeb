package com.socialweb.web;

import com.socialweb.dto.PostSummary;
import com.socialweb.dto.UserSummary;
import com.socialweb.service.LeaderboardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/posts")
    public List<PostSummary> posts(@RequestParam(defaultValue = "10") int limit) {
        return leaderboardService.posts(Math.min(Math.max(limit, 1), 50), AuthHelper.optionalUser());
    }

    @GetMapping("/users")
    public List<UserSummary> users(@RequestParam(defaultValue = "10") int limit) {
        return leaderboardService.users(Math.min(Math.max(limit, 1), 50));
    }
}