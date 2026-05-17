package com.campushub.order.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderListItem(
        @JsonSerialize(using = ToStringSerializer.class)
        Long orderId,
        @JsonSerialize(using = ToStringSerializer.class)
        Long reqId,
        String reqTitle,
        @JsonSerialize(using = ToStringSerializer.class)
        Long receiverId,
        String receiverName,
        BigDecimal amount,
        String status,
        LocalDateTime createdAt
) {
}
