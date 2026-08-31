package com.socialweb.web;

import com.socialweb.dto.AuthRequests;
import com.socialweb.dto.AuthResponse;
import com.socialweb.dto.UserDto;
import com.socialweb.security.CurrentUser;
import com.socialweb.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody AuthRequests.Register req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequests.Login req) {
        return authService.login(req);
    }

    @GetMapping("/me")
    public UserDto me() {
        return authService.me(AuthHelper.requireUser());
    }
}
