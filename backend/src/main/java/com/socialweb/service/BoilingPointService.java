package com.socialweb.service;

import com.socialweb.dto.BoilingDto;
import com.socialweb.dto.BoilingRequests;
import com.socialweb.entity.BoilingLike;
import com.socialweb.entity.BoilingPoint;
import com.socialweb.entity.NotificationType;
import com.socialweb.entity.User;
import com.socialweb.repository.BoilingLikeRepository;
import com.socialweb.repository.BoilingPointRepository;
import com.socialweb.repository.UserFollowRepository;
import com.socialweb.repository.UserRepository;
import com.socialweb.web.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** 沸点：掘金式短内容动态流 */
@Service
public class BoilingPointService {

    private final BoilingPointRepository boilingRepository;
    private final BoilingLikeRepository boilingLikeRepository;
    private final UserRepository userRepository;
    private final UserFollowRepository followRepository;
    private final NotificationService notificationService;
    private final Mapper mapper;

    public BoilingPointService(BoilingPointRepository boilingRepository,
                               BoilingLikeRepository boilingLikeRepository,
                               UserRepository userRepository,
                               UserFollowRepository followRepository,
                               NotificationService notificationService,
                               Mapper mapper) {
        this.boilingRepository = boilingRepository;
        this.boilingLikeRepository = boilingLikeRepository;
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.notificationService = notificationService;
        this.mapper = mapper;
    }

    @Transactional
    public BoilingDto create(Long userId, BoilingRequests.Create req) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
        BoilingPoint b = new BoilingPoint();
        b.setAuthor(author);
        b.setContent(req.content.trim());
        b.setImageUrl(req.imageUrl == null ? null : req.imageUrl.trim());
        BoilingPoint saved = boilingRepository.save(b);
        LevelSystem.award(author, LevelSystem.BOILING_CREATE);
        return toDto(saved, false);
    }

    @Transactional(readOnly = true)
    public Page<BoilingDto> list(String feed, Long viewerId, Pageable pageable) {
        List<BoilingPoint> all = boilingRepository.findAll();
        String f = feed == null || feed.isBlank() ? "latest" : feed.toLowerCase();
        List<BoilingPoint> sorted = switch (f) {
            case "hot" -> all.stream()
                    .sorted(Comparator.comparingLong(BoilingPoint::getLikeCount).reversed())
                    .toList();
            case "following" -> {
                if (viewerId == null) yield List.<BoilingPoint>of();
                Set<Long> ids = followRepository.findByFollowerId(viewerId).stream()
                        .map(x -> x.getFollowee().getId()).collect(Collectors.toSet());
                yield all.stream().filter(b -> ids.contains(b.getAuthor().getId()))
                        .sorted(Comparator.comparing(BoilingPoint::getCreatedAt).reversed()).toList();
            }
            default -> all.stream()
                    .sorted(Comparator.comparing(BoilingPoint::getCreatedAt).reversed()).toList();
        };
        int start = (int) Math.min(pageable.getOffset(), sorted.size());
        int end = Math.min(start + pageable.getPageSize(), sorted.size());
        List<BoilingPoint> slice = sorted.subList(start, end);
        Set<Long> liked = viewerId == null || slice.isEmpty() ? Set.of()
                : boilingLikeRepository.findLikedBoilingIds(viewerId,
                        slice.stream().map(BoilingPoint::getId).toList());
        List<BoilingDto> dtos = slice.stream().map(b -> toDto(b, liked.contains(b.getId()))).toList();
        return new PageImpl<>(dtos, pageable, sorted.size());
    }

    @Transactional
    public Map<String, Boolean> toggleLike(Long userId, Long boilingId) {
        BoilingPoint b = boilingRepository.findById(boilingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "沸点不存在"));
        User u = userRepository.getReferenceById(userId);
        Optional<BoilingLike> existing = boilingLikeRepository.findByUserIdAndBoilingPointId(userId, boilingId);
        if (existing.isPresent()) {
            boilingLikeRepository.delete(existing.get());
            b.setLikeCount(Math.max(0, b.getLikeCount() - 1));
            boilingRepository.save(b);
            return Map.of("liked", false);
        }
        BoilingLike bl = new BoilingLike();
        bl.setUser(u);
        bl.setBoilingPoint(b);
        boilingLikeRepository.save(bl);
        b.setLikeCount(b.getLikeCount() + 1);
        boilingRepository.save(b);
        notificationService.create(b.getAuthor(), u, NotificationType.BOILING_LIKE, null);
        return Map.of("liked", true);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        BoilingPoint b = boilingRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "沸点不存在"));
        if (!b.getAuthor().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "只有作者可以删除");
        }
        boilingLikeRepository.deleteByBoilingPointId(id);
        boilingRepository.delete(b);
    }

    private BoilingDto toDto(BoilingPoint b, boolean liked) {
        BoilingDto d = new BoilingDto();
        d.setId(b.getId());
        d.setContent(b.getContent());
        d.setImageUrl(b.getImageUrl());
        d.setLikeCount(b.getLikeCount());
        d.setCommentCount(b.getCommentCount());
        d.setLiked(liked);
        d.setAuthor(mapper.toUserSummary(b.getAuthor()));
        d.setCreatedAt(b.getCreatedAt());
        return d;
    }
}