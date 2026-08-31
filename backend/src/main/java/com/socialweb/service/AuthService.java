package com.socialweb.service;

import com.socialweb.dto.AuthRequests;
import com.socialweb.dto.AuthResponse;
import com.socialweb.dto.UserDto;
import com.socialweb.entity.User;
import com.socialweb.repository.UserRepository;
import com.socialweb.security.JwtService;
import com.socialweb.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Mapper mapper;

    public AuthService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       Mapper mapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mapper = mapper;
    }

    @Transactional
    public AuthResponse register(AuthRequests.Register req) {
        if (userRepository.existsByUsername(req.username)) {
            throw new ApiException(HttpStatus.CONFLICT, "用户名已存在");
        }
        User u = new User();
        u.setUsername(req.username);
        u.setPassword(passwordEncoder.encode(req.password));
        u.setNickname(req.nickname);
        u.setAvatar("https://api.dicebear.com/7.x/thumbs/svg?seed=" + req.username);
        User saved = userRepository.save(u);
        String token = jwtService.generate(saved.getId(), saved.getUsername());
        return new AuthResponse(token, mapper.toUserDto(saved, saved.getId()));
    }

    @Transactional
    public AuthResponse login(AuthRequests.Login req) {
        User u = userRepository.findByUsername(req.username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
        if (!passwordEncoder.matches(req.password, u.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        String token = jwtService.generate(u.getId(), u.getUsername());
        return new AuthResponse(token, mapper.toUserDto(u, u.getId()));
    }

    @Transactional(readOnly = true)
    public UserDto me(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
        return mapper.toUserDto(u, userId);
    }
}
