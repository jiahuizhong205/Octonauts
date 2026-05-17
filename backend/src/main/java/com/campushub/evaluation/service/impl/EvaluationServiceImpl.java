package com.campushub.evaluation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campushub.common.exception.BusinessException;
import com.campushub.entity.BizEvaluation;
import com.campushub.entity.BizOrder;
import com.campushub.entity.BizRequirement;
import com.campushub.entity.OrderStatus;
import com.campushub.entity.SysUser;
import com.campushub.evaluation.dto.EvaluationSubmitReq;
import com.campushub.evaluation.service.EvaluationService;
import com.campushub.mapper.BizEvaluationMapper;
import com.campushub.mapper.BizOrderMapper;
import com.campushub.mapper.BizRequirementMapper;
import com.campushub.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationServiceImpl implements EvaluationService {

    @Autowired
    private BizOrderMapper bizOrderMapper;

    @Autowired
    private BizRequirementMapper bizRequirementMapper;

    @Autowired
    private BizEvaluationMapper bizEvaluationMapper;

    // 注入用户表 Mapper，用于更新信用分
    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class) // 开启事务：评价和扣分必须同时成功或同时失败
    public void submitEvaluation(Long orderId, Long userId, EvaluationSubmitReq req) {
        // 1. 查询订单信息
        BizOrder order = bizOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "查询的订单不存在");
        }

        // 2. 校验订单状态是否已完成
        if (!OrderStatus.COMPLETED.name().equals(order.getStatus())) {
            throw new BusinessException(4003, "订单尚未完成，暂不可提交评价");
        }

        // 3. 查询需求信息，获取发布者 ID
        BizRequirement requirement = bizRequirementMapper.selectById(order.getReqId());
        if (requirement == null) {
            throw new BusinessException(404, "订单关联的需求数据异常");
        }
        Long publisherId = requirement.getPublisherId();

        // 4. 校验当前用户是否有权评价
        if (!userId.equals(publisherId) && !userId.equals(order.getReceiverId())) {
            throw new BusinessException(4004, "您不是该订单的参与者，无权评价");
        }

        // 5. 校验是否重复评价
        LambdaQueryWrapper<BizEvaluation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BizEvaluation::getOrderId, orderId)
                    .eq(BizEvaluation::getReviewerId, userId);
        if (bizEvaluationMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException(400, "您已对该订单进行过评价");
        }

        // 6. 保存评价数据
        BizEvaluation evaluation = new BizEvaluation();
        evaluation.setOrderId(orderId);
        evaluation.setReviewerId(userId);
        
        // 确定被评价人（Target ID）
        Long targetId = userId.equals(publisherId) ? order.getReceiverId() : publisherId;
        evaluation.setTargetId(targetId);
        evaluation.setStar(req.getStar());
        evaluation.setContent(req.getContent());
        
        bizEvaluationMapper.insert(evaluation);

        // =======================
        // 7. 信用分结算引擎核心逻辑
        // =======================
        SysUser targetUser = sysUserMapper.selectById(targetId);
        if (targetUser != null) {
            int scoreChange = 0;
            switch (req.getStar()) {
                case 5: scoreChange = 2; break;
                case 4: scoreChange = 1; break;
                case 3: scoreChange = 0; break; 
                case 2: scoreChange = -1; break;
                case 1: scoreChange = -2; break;
            }

            if (scoreChange != 0) {
                // 防御性编程：处理新用户信用分可能为 null 的情况（假设初始满分 100）
                int currentScore = targetUser.getCreditScore() != null ? targetUser.getCreditScore() : 100;
                int newScore = currentScore + scoreChange;
                
                // 确保信用分不会扣成负数
                if (newScore < 0) newScore = 0;
                
                targetUser.setCreditScore(newScore);
                sysUserMapper.updateById(targetUser);
            }
        }
    }
}