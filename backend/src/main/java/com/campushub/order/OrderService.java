package com.campushub.order;

import com.campushub.common.exception.ApiCode;
import com.campushub.common.exception.BusinessException;
import com.campushub.entity.BizOrder;
import com.campushub.entity.BizRequirement;
import com.campushub.mapper.BizOrderMapper;
import com.campushub.mapper.BizRequirementMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class OrderService {
    private final JdbcClient jdbcClient;
    private final BizOrderMapper bizOrderMapper;
    private final BizRequirementMapper bizRequirementMapper;

    public OrderService(JdbcClient jdbcClient, BizOrderMapper bizOrderMapper,
                        BizRequirementMapper bizRequirementMapper) {
        this.jdbcClient = jdbcClient;
        this.bizOrderMapper = bizOrderMapper;
        this.bizRequirementMapper = bizRequirementMapper;
    }

    @Transactional
    public Long createOrder(Long reqId, Long receiverId) {
        var row = jdbcClient.sql("""
                        SELECT publisher_id, budget, status
                        FROM biz_requirement WHERE req_id = :reqId
                        """)
                .param("reqId", reqId)
                .query((rs, rowNum) -> new Object[]{
                        rs.getLong("publisher_id"),
                        rs.getBigDecimal("budget"),
                        rs.getString("status")
                })
                .optional()
                .orElseThrow(() -> new BusinessException(404, "需求不存在"));

        Long publisherId = (Long) row[0];
        BigDecimal budget = (BigDecimal) row[1];
        String status = (String) row[2];

        if (publisherId.equals(receiverId)) {
            throw new BusinessException(ApiCode.SELF_ORDER);
        }
        if (!"PENDING".equals(status)) {
            throw new BusinessException(ApiCode.ORDER_TAKEN);
        }

        BizOrder order = new BizOrder();
        order.setReqId(reqId);
        order.setReceiverId(receiverId);
        order.setAmount(budget);
        order.setStatus("IN_PROGRESS");

        try {
            bizOrderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ApiCode.ORDER_TAKEN);
        }

        BizRequirement req = new BizRequirement();
        req.setReqId(reqId);
        req.setStatus("ACCEPTED");
        bizRequirementMapper.updateById(req);

        return order.getOrderId();
    }
}
