package com.campushub.requirement;

import com.campushub.common.exception.BusinessException;
import com.campushub.requirement.dto.PageResponse;
import com.campushub.requirement.dto.RequirementDetailResponse;
import com.campushub.requirement.dto.RequirementListItem;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class RequirementService {
    private final JdbcClient jdbcClient;

    public RequirementService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public PageResponse<RequirementListItem> listRequirements(
            String keyword,
            String type,
            String status,
            Integer page,
            Integer pageSize
    ) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null ? 10 : Math.min(Math.max(pageSize, 1), 50);
        int offset = (safePage - 1) * safePageSize;

        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        Map<String, Object> params = new HashMap<>();

        if (StringUtils.hasText(keyword)) {
            where.append(" AND (r.title LIKE :keyword OR r.description LIKE :keyword)");
            params.put("keyword", "%" + keyword.trim() + "%");
        }
        if (StringUtils.hasText(type)) {
            where.append(" AND r.type = :type");
            params.put("type", type.trim());
        }
        if (StringUtils.hasText(status)) {
            where.append(" AND r.status = :status");
            params.put("status", status.trim());
        }

        Long total = jdbcClient.sql("SELECT COUNT(*) FROM biz_requirement r" + where)
                .params(params)
                .query(Long.class)
                .single();

        params.put("limit", safePageSize);
        params.put("offset", offset);
        var list = jdbcClient.sql("""
                        SELECT r.req_id, r.title, r.budget, r.type, r.status, r.created_at,
                               COALESCE(u.nickname, u.username) AS publisher_name
                        FROM biz_requirement r
                        LEFT JOIN sys_user u ON r.publisher_id = u.user_id
                        """ + where + " ORDER BY r.created_at DESC LIMIT :limit OFFSET :offset")
                .params(params)
                .query((rs, rowNum) -> new RequirementListItem(
                        rs.getLong("req_id"),
                        rs.getString("title"),
                        rs.getBigDecimal("budget"),
                        rs.getString("type"),
                        rs.getString("status"),
                        rs.getString("publisher_name"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ))
                .list();

        return new PageResponse<>(total == null ? 0 : total, safePage, safePageSize, list);
    }

    public RequirementDetailResponse getRequirementDetail(Long reqId) {
        return jdbcClient.sql("""
                        SELECT r.req_id, r.publisher_id, r.title, r.description, r.budget, r.type,
                               r.status, r.created_at, COALESCE(u.nickname, u.username) AS publisher_name
                        FROM biz_requirement r
                        LEFT JOIN sys_user u ON r.publisher_id = u.user_id
                        WHERE r.req_id = :reqId
                        """)
                .param("reqId", reqId)
                .query((rs, rowNum) -> new RequirementDetailResponse(
                        rs.getLong("req_id"),
                        rs.getLong("publisher_id"),
                        rs.getString("publisher_name"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getBigDecimal("budget"),
                        rs.getString("type"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        // 详情页据此决定是否展示接单入口，避免已接单/已关闭需求继续被操作。
                        "PENDING".equals(rs.getString("status"))
                ))
                .optional()
                .orElseThrow(() -> new BusinessException(404, "需求不存在"));
    }
}
