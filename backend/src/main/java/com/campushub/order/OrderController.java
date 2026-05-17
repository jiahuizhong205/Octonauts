package com.campushub.order;

import com.campushub.common.api.ApiResponse;
import com.campushub.order.dto.CreateOrderRequest;
import com.campushub.order.dto.OrderDetailResponse;
import com.campushub.order.dto.OrderListItem;
import com.campushub.requirement.dto.PageResponse;
import com.campushub.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping
    public ApiResponse<PageResponse<OrderListItem>> listOrders(
            @RequestParam(defaultValue = "received") String tab,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            HttpServletRequest httpRequest
    ) {
        Long userId = currentUser.requireUserId(httpRequest);
        return ApiResponse.success(orderService.listRelatedOrders(tab, userId, page, pageSize));
    }

    @GetMapping("/{orderId:\\d+}")
    public ApiResponse<OrderDetailResponse> getOrderDetail(
            @PathVariable Long orderId,
            HttpServletRequest httpRequest
    ) {
        Long userId = currentUser.requireUserId(httpRequest);
        return ApiResponse.success(orderService.getOrderDetail(orderId, userId));
    }
}
