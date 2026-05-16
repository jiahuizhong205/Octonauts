package com.campushub.profile;

import com.campushub.common.exception.BusinessException;
import com.campushub.profile.dto.ProfileResponse;
import com.campushub.profile.dto.ProfileUpdateRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {
    private final JdbcClient jdbcClient;

    public ProfileService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public ProfileResponse getProfile(Long currentUserId) {
        return jdbcClient.sql("""
                        SELECT user_id, username, nickname, student_id, campus, college, major, grade,
                               bio, contact_visible, credit_score
                        FROM sys_user
                        WHERE user_id = :userId
                        """)
                .param("userId", currentUserId)
                .query((rs, rowNum) -> new ProfileResponse(
                        rs.getLong("user_id"),
                        rs.getString("username"),
                        rs.getString("nickname"),
                        rs.getString("student_id"),
                        rs.getString("campus"),
                        rs.getString("college"),
                        rs.getString("major"),
                        rs.getString("grade"),
                        rs.getString("bio"),
                        rs.getBoolean("contact_visible"),
                        rs.getInt("credit_score")
                ))
                .optional()
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
    }

    public ProfileResponse updateProfile(Long currentUserId, ProfileUpdateRequest request) {
        int updated = jdbcClient.sql("""
                        UPDATE sys_user
                        SET nickname = :nickname,
                            campus = :campus,
                            college = :college,
                            major = :major,
                            grade = :grade,
                            bio = :bio,
                            contact_visible = :contactVisible
                        WHERE user_id = :userId
                        """)
                .param("nickname", request.nickname())
                .param("campus", request.campus())
                .param("college", request.college())
                .param("major", request.major())
                .param("grade", request.grade())
                .param("bio", request.bio())
                .param("contactVisible", Boolean.TRUE.equals(request.contactVisible()))
                .param("userId", currentUserId)
                .update();
        if (updated == 0) {
            throw new BusinessException(404, "用户不存在");
        }
        return getProfile(currentUserId);
    }
}
