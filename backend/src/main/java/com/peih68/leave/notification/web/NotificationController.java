package com.peih68.leave.notification.web;

import com.peih68.leave.auth.domain.UserPrincipal;
import com.peih68.leave.common.web.ApiResponse;
import com.peih68.leave.notification.service.NotificationService;
import com.peih68.leave.notification.web.dto.NotificationResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** A user's own notifications — every endpoint is scoped to the authenticated principal. */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<Page<NotificationResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<NotificationResponse> page = notificationService.list(principal.getId(), unreadOnly, pageable);
        return ApiResponse.ok(page, Map.of(
                "page", page.getNumber(),
                "size", page.getSize(),
                "totalElements", page.getTotalElements(),
                "totalPages", page.getTotalPages()));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(notificationService.unreadCount(principal.getId()));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markRead(
            @PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(notificationService.markRead(id, principal.getId()));
    }

    @PostMapping("/read-all")
    public ApiResponse<Integer> markAllRead(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(notificationService.markAllRead(principal.getId()));
    }
}
