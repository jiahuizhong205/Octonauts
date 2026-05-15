package com.campushub.notification;

import com.campushub.common.ApiResponse;
import com.campushub.notification.dto.NotificationItem;
import com.campushub.notification.dto.UnreadCountResponse;
import com.campushub.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    public NotificationController(NotificationService notificationService, CurrentUser currentUser) {
        this.notificationService = notificationService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ApiResponse<List<NotificationItem>> listNotifications(
            HttpServletRequest request,
            @RequestParam(required = false) Boolean unreadOnly
    ) {
        Long currentUserId = currentUser.requireUserId(request);
        return ApiResponse.success(notificationService.listNotifications(currentUserId, unreadOnly));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> getUnreadCount(HttpServletRequest request) {
        Long currentUserId = currentUser.requireUserId(request);
        return ApiResponse.success(notificationService.getUnreadCount(currentUserId));
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(HttpServletRequest request, @PathVariable Long notificationId) {
        Long currentUserId = currentUser.requireUserId(request);
        // 更新条件同时带 user_id，保证用户不能把别人的通知标成已读。
        notificationService.markAsRead(currentUserId, notificationId);
        return ApiResponse.success("通知已读", null);
    }

    @PatchMapping("/read-all")
    public ApiResponse<Map<String, Integer>> markAllAsRead(HttpServletRequest request) {
        Long currentUserId = currentUser.requireUserId(request);
        int updated = notificationService.markAllAsRead(currentUserId);
        return ApiResponse.success("全部通知已读", Map.of("updated", updated));
    }
}
