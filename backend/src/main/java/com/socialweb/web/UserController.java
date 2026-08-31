package com.socialweb.web;

import com.socialweb.dto.PostRequests;
import com.socialweb.dto.ProfileRequests;
import com.socialweb.dto.UserDto;
import com.socialweb.dto.UserSummary;
import com.socialweb.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me/bookmarks")
    public org.springframework.data.domain.Page<com.socialweb.dto.PostSummary> bookmarks(
            @PageableDefault(size = 20) Pageable pageable) {
        return userService.bookmarks(AuthHelper.requireUser(), pageable);
    }

    @PutMapping("/profile")
    public UserDto updateProfile(@Valid @RequestBody ProfileRequests.Update req) {
        return userService.updateProfile(AuthHelper.requireUser(), req);
    }

    @GetMapping("/{id}")
    public UserDto get(@PathVariable Long id) {
        return userService.get(id, AuthHelper.optionalUser());
    }

    @PostMapping("/{id}/follow")
    public Map<String, Boolean> follow(@PathVariable Long id) {
        return userService.toggleFollow(AuthHelper.requireUser(), id);
    }

    @GetMapping("/{id}/following")
    public Page<UserSummary> following(@PathVariable Long id,
                                       @PageableDefault(size = 20) Pageable pageable) {
        return userService.following(id, pageable);
    }

    @GetMapping("/{id}/followers")
    public Page<UserSummary> followers(@PathVariable Long id,
                                       @PageableDefault(size = 20) Pageable pageable) {
        return userService.followers(id, pageable);
    }
}
