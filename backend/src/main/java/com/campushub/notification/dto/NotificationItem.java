package com.campushub.notification.dto;

import java.time.LocalDateTime;

public record NotificationItem(
        Long notificationId,
        String title,
        String content,
        String eventType,
        boolean read,
        LocalDateTime createdAt
) {
}
