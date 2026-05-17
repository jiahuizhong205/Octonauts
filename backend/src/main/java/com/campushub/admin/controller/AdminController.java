package com.campushub.admin.controller;

import com.campushub.admin.service.AdminService;
import com.campushub.common.api.ApiResponse;
import com.campushub.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private CurrentUser currentUser;

    @PutMapping("/requirements/{reqId}/cancel")
    public ApiResponse<Void> cancelRequirement(
            @PathVariable Long reqId,
            HttpServletRequest request) {
        
        // 利用安全工具获取当前用户 ID
        Long adminId = currentUser.requireUserId(request);
        
        adminService.cancelRequirement(reqId, adminId);
        return ApiResponse.success("违规需求已成功下架", null);
    }
}