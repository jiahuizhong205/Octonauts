package com.campushub.requirement;

import com.campushub.common.ApiResponse;
import com.campushub.requirement.dto.PageResponse;
import com.campushub.requirement.dto.RequirementDetailResponse;
import com.campushub.requirement.dto.RequirementListItem;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/requirements")
public class RequirementController {
    private final RequirementService requirementService;

    public RequirementController(RequirementService requirementService) {
        this.requirementService = requirementService;
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

    @GetMapping("/{reqId}")
    public ApiResponse<RequirementDetailResponse> getRequirementDetail(@PathVariable Long reqId) {
        return ApiResponse.success(requirementService.getRequirementDetail(reqId));
    }
}
