package com.campushub.profile;

import com.campushub.common.api.ApiResponse;
import com.campushub.profile.dto.ProfileResponse;
import com.campushub.profile.dto.ProfileUpdateRequest;
import com.campushub.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class ProfileController {
    private final ProfileService profileService;
    private final CurrentUser currentUser;

    public ProfileController(ProfileService profileService, CurrentUser currentUser) {
        this.profileService = profileService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ApiResponse<ProfileResponse> getProfile(HttpServletRequest request) {
        Long currentUserId = currentUser.requireUserId(request);
        return ApiResponse.success(profileService.getProfile(currentUserId));
    }

    @PutMapping
    public ApiResponse<ProfileResponse> updateProfile(
            HttpServletRequest request,
            @Valid @RequestBody ProfileUpdateRequest updateRequest
    ) {
        Long currentUserId = currentUser.requireUserId(request);
        // 只允许更新“我”的资料，避免前端传 userId 后产生越权修改。
        return ApiResponse.success("个人资料更新成功", profileService.updateProfile(currentUserId, updateRequest));
    }
}
