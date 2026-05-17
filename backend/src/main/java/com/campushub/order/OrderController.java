package com.campushub.order;

import com.campushub.common.api.ApiResponse;
import com.campushub.order.dto.CreateOrderRequest;
import com.campushub.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orderService;
    private final CurrentUser currentUser;

    public OrderController(OrderService orderService, CurrentUser currentUser) {
        this.orderService = orderService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ApiResponse<Map<String, Long>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = currentUser.requireUserId(httpRequest);
        Long orderId = orderService.createOrder(request.reqId(), userId);
        return ApiResponse.success("接单成功", Map.of("orderId", orderId));
    }
}
