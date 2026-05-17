package com.campushub.order.dto;

import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotNull(message = "需求ID不能为空")
        Long reqId
) {
}
