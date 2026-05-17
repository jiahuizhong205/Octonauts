package com.campushub.requirement;

import com.campushub.common.api.ApiResponse;
import com.campushub.requirement.dto.CreateRequirementRequest;
import com.campushub.requirement.dto.PageResponse;
import com.campushub.requirement.dto.RequirementDetailResponse;
import com.campushub.requirement.dto.RequirementListItem;
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
@RequestMapping("/api/v1/requirements")
public class RequirementController {
    private final RequirementService requirementService;
    private final CurrentUser currentUser;

    public RequirementController(RequirementService requirementService, CurrentUser currentUser) {
        this.requirementService = requirementService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ApiResponse<Map<String, Long>> createRequirement(
            @Valid @RequestBody CreateRequirementRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = currentUser.requireUserId(httpRequest);
        Long reqId = requirementService.createRequirement(request, userId);
        return ApiResponse.success("发布成功", Map.of("reqId", reqId));
    }

    @GetMapping
    public ApiResponse<PageResponse<RequirementListItem>> listRequirements(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.success(requirementService.listRequirements(keyword, type, status, page, pageSize));
    }

    @GetMapping("/{reqId:\\d+}")
    public ApiResponse<RequirementDetailResponse> getRequirementDetail(@PathVariable Long reqId) {
        return ApiResponse.success(requirementService.getRequirementDetail(reqId));
    }
}
