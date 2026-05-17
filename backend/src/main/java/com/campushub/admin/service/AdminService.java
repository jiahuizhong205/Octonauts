package com.campushub.admin.service;

public interface AdminService {
    /**
     * 下架(取消)违规需求
     */
    void cancelRequirement(Long reqId, Long adminId);
}