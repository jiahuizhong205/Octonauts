package com.campushub.evaluation.controller;

import com.campushub.common.api.ApiResponse;
import com.campushub.evaluation.dto.EvaluationSubmitReq;
import com.campushub.evaluation.service.EvaluationService;
import com.campushub.security.CurrentUser; 
import jakarta.servlet.http.HttpServletRequest; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class EvaluationController {

    @Autowired
    private EvaluationService evaluationService;

    // 1. 注入CurrentUser 工具
    @Autowired
    private CurrentUser currentUser;

    @PostMapping("/{orderId}/evaluations")
    public ApiResponse<Void> submitEvaluation(
            @PathVariable Long orderId,
            HttpServletRequest request, // 2. 方法参数里直接要一个原生的 request
            @Validated @RequestBody EvaluationSubmitReq req) {
        
        // 3. 使用工具，安全地提取 userId，自带未登录拦截功能！
        Long userId = currentUser.requireUserId(request);
        
        evaluationService.submitEvaluation(orderId, userId, req);
        return ApiResponse.success("评价提交成功", null);
    }
}