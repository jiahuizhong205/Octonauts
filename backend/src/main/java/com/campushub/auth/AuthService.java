package com.campushub.auth;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campushub.auth.dto.LoginRequest;
import com.campushub.auth.dto.LoginResponse;
import com.campushub.auth.dto.RegisterRequest;
import com.campushub.common.exception.ApiCode;
import com.campushub.common.exception.BusinessException;
import com.campushub.entity.SysUser;
import com.campushub.mapper.SysUserMapper;
import com.campushub.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public void register(RegisterRequest request) {
        boolean exists = sysUserMapper.exists(new QueryWrapper<SysUser>()
                .eq("username", request.getUsername()));
        if (exists) {
            throw new BusinessException(ApiCode.USERNAME_TAKEN);
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStudentId(StringUtils.hasText(request.getStudentId()) ? request.getStudentId() : "U_" + request.getUsername());
        user.setPhoneEncrypted("");
        user.setCampus(request.getCampus());
        user.setCreditScore(100);

        sysUserMapper.insert(user);
    }

    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserMapper.selectOne(new QueryWrapper<SysUser>()
                .eq("username", request.getUsername()));
        if (user == null) {
            throw new BusinessException(ApiCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ApiCode.PASSWORD_ERROR);
        }

        String token = jwtUtil.generateToken(user.getUserId());
        return LoginResponse.builder()
                .userId(user.getUserId())
                .token(token)
                .build();
    }
}
