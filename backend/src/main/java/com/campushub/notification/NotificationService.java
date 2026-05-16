package com.campushub.notification;

import com.campushub.common.exception.BusinessException;
import com.campushub.notification.dto.NotificationItem;
import com.campushub.notification.dto.UnreadCountResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class NotificationService {
    private final JdbcClient jdbcClient;

    public NotificationService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<NotificationItem> listNotifications(Long currentUserId, Boolean unreadOnly) {
        String unreadFilter = Boolean.TRUE.equals(unreadOnly) ? " AND read_status = 0" : "";
        return jdbcClient.sql("""
                        SELECT notification_id, title, content, event_type, read_status, created_at
                        FROM biz_notification
                        WHERE user_id = :userId
                        """ + unreadFilter + " ORDER BY created_at DESC")
                .param("userId", currentUserId)
                .query((rs, rowNum) -> new NotificationItem(
                        rs.getLong("notification_id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getString("event_type"),
                        rs.getBoolean("read_status"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ))
                .list();
    }

    public UnreadCountResponse getUnreadCount(Long currentUserId) {
        Long count = jdbcClient.sql("""
                        SELECT COUNT(*)
                        FROM biz_notification
                        WHERE user_id = :userId AND read_status = 0
                        """)
                .param("userId", currentUserId)
                .query(Long.class)
                .single();
        return new UnreadCountResponse(count == null ? 0 : count);
    }

    public void markAsRead(Long currentUserId, Long notificationId) {
        int updated = jdbcClient.sql("""
                        UPDATE biz_notification
                        SET read_status = 1
                        WHERE notification_id = :notificationId AND user_id = :userId
                        """)
                .param("notificationId", notificationId)
                .param("userId", currentUserId)
                .update();
        if (updated == 0) {
            throw new BusinessException(404, "通知不存在");
        }
    }

    public int markAllAsRead(Long currentUserId) {
        return jdbcClient.sql("""
                        UPDATE biz_notification
                        SET read_status = 1
                        WHERE user_id = :userId AND read_status = 0
                        """)
                .param("userId", currentUserId)
                .update();
    }

    public void createNotification(Long userId, String title, String content, String eventType) {
        long notificationId = LocalDateTime.now()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        jdbcClient.sql("""
                        INSERT INTO biz_notification(notification_id, user_id, title, content, event_type)
                        VALUES (:notificationId, :userId, :title, :content, :eventType)
                        """)
                .param("notificationId", notificationId)
                .param("userId", userId)
                .param("title", title)
                .param("content", content)
                .param("eventType", eventType)
                .update();
    }
}
