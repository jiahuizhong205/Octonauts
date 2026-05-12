package com.campushub.profile.dto;

public record ProfileResponse(
        Long userId,
        String username,
        String nickname,
        String studentId,
        String campus,
        String college,
        String major,
        String grade,
        String bio,
        Boolean contactVisible,
        Integer creditScore
) {
}
