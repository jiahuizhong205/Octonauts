package com.campushub.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderDetailResponse(
        Long orderId,
        Long reqId,
        String reqTitle,
        String reqDescription,
        Long publisherId,
        String publisherName,
        Long receiverId,
        String receiverName,
        BigDecimal amount,
        String status,
        LocalDateTime createdAt,
        LocalDateTime finishedAt
) {
}
