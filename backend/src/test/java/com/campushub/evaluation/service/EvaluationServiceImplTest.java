package com.campushub.evaluation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campushub.common.exception.BusinessException;
import com.campushub.entity.BizOrder;
import com.campushub.entity.BizRequirement;
import com.campushub.entity.OrderStatus;
import com.campushub.entity.SysUser;
import com.campushub.evaluation.dto.EvaluationSubmitReq;
import com.campushub.evaluation.service.impl.EvaluationServiceImpl;
import com.campushub.mapper.BizEvaluationMapper;
import com.campushub.mapper.BizOrderMapper;
import com.campushub.mapper.BizRequirementMapper;
import com.campushub.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EvaluationServiceImplTest {

    @Mock
    private BizOrderMapper bizOrderMapper;
    @Mock
    private BizRequirementMapper bizRequirementMapper;
    @Mock
    private BizEvaluationMapper bizEvaluationMapper;
    @Mock
    private SysUserMapper sysUserMapper;

    @InjectMocks
    private EvaluationServiceImpl evaluationService;

    private BizOrder mockOrder;
    private BizRequirement mockReq;
    private SysUser mockTargetUser;
    private EvaluationSubmitReq mockReqDto;

    @BeforeEach
    void setUp() {
        // 初始化一些通用的前置模拟数据
        mockOrder = new BizOrder();
        mockOrder.setOrderId(1L);
        mockOrder.setReqId(10L);
        mockOrder.setStatus(OrderStatus.COMPLETED.name());
        mockOrder.setReceiverId(200L); // 接单人 ID

        mockReq = new BizRequirement();
        mockReq.setReqId(10L);
        mockReq.setPublisherId(100L); // 发布人 ID

        mockTargetUser = new SysUser();
        mockTargetUser.setUserId(200L);
        mockTargetUser.setCreditScore(90); // 初始信用分设为 90

        mockReqDto = new EvaluationSubmitReq();
    }

    @Test
    void testSubmitEvaluation_5Stars_ShouldAdd2Points() {
        // 1. 准备数据：发布人(100) 给 接单人(200) 打 5 星
        mockReqDto.setStar(5);
        mockReqDto.setContent("太棒了！");

        // 2. 模拟数据库的返回结果
        when(bizOrderMapper.selectById(1L)).thenReturn(mockOrder);
        when(bizRequirementMapper.selectById(10L)).thenReturn(mockReq);
        when(bizEvaluationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(sysUserMapper.selectById(200L)).thenReturn(mockTargetUser);

        // 3. 执行要测试的业务逻辑
        evaluationService.submitEvaluation(1L, 100L, mockReqDto);

        // 4. 断言验证（核心！）
        // 捕捉 sysUserMapper.updateById 传入的参数，看看分数是不是真的加了 2 分
        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper, times(1)).updateById(userCaptor.capture());
        
        SysUser updatedUser = userCaptor.getValue();
        assertEquals(92, updatedUser.getCreditScore(), "5星好评应该加2分 (90+2=92)");
    }

    @Test
    void testSubmitEvaluation_1Star_ShouldDeduct2PointsAndNotBelowZero() {
        // 1. 准备极端数据：接单人只有 1 分，此时被打了 1 星（本应扣2分，但不能跌破0分）
        mockTargetUser.setCreditScore(1); 
        mockReqDto.setStar(1);

        when(bizOrderMapper.selectById(1L)).thenReturn(mockOrder);
        when(bizRequirementMapper.selectById(10L)).thenReturn(mockReq);
        when(bizEvaluationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(sysUserMapper.selectById(200L)).thenReturn(mockTargetUser);

        // 3. 执行逻辑
        evaluationService.submitEvaluation(1L, 100L, mockReqDto);

        // 4. 断言验证
        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper, times(1)).updateById(userCaptor.capture());
        
        SysUser updatedUser = userCaptor.getValue();
        assertEquals(0, updatedUser.getCreditScore(), "1星极差扣2分，但信用分最低只能到0分，不能为负数");
    }

    @Test
    void testSubmitEvaluation_OrderNotCompleted_ShouldThrowException() {
        // 准备数据：订单处于“进行中”状态
        mockOrder.setStatus(OrderStatus.IN_PROGRESS.name());
        when(bizOrderMapper.selectById(1L)).thenReturn(mockOrder);

        // 执行并断言：应该抛出 BusinessException 且错误码为 4003
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            evaluationService.submitEvaluation(1L, 100L, mockReqDto);
        });
        
        assertEquals(4003, exception.getCode());
    }
}