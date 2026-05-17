package com.campushub.evaluation.service;

import com.campushub.evaluation.dto.EvaluationSubmitReq;

public interface EvaluationService {
    /**
     * 提交订单评价
     */
    void submitEvaluation(Long orderId, Long userId, EvaluationSubmitReq req);
}