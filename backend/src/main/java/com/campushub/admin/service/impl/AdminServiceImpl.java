package com.campushub.admin.service.impl;

import com.campushub.admin.service.AdminService;
import com.campushub.common.exception.BusinessException;
import com.campushub.entity.BizRequirement;
import com.campushub.entity.SysUser;
import com.campushub.mapper.BizRequirementMapper;
import com.campushub.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private BizRequirementMapper bizRequirementMapper;

    @Override
    public void cancelRequirement(Long reqId, Long adminId) {
        // 1. 防御：查询当前操作人的角色
        SysUser user = sysUserMapper.selectById(adminId);
        if (user == null || user.getRole() == null || user.getRole() != 1) {
            throw new BusinessException(403, "越权操作：仅系统管理员可执行此操作");
        }

        // 2. 查询目标需求
        BizRequirement req = bizRequirementMapper.selectById(reqId);
        if (req == null) {
            throw new BusinessException(404, "目标需求不存在");
        }

        // 3. 执行下架（修改状态为 CANCELED）
        req.setStatus("CANCELED");
        bizRequirementMapper.updateById(req);
    }
}