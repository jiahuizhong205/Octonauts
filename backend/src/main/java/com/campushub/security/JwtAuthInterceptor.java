package com.campushub.security;

import com.campushub.common.exception.ApiCode;
import com.campushub.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(ApiCode.UNAUTHORIZED);
        }

        String token = authHeader.substring("Bearer ".length()).trim();
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException(ApiCode.UNAUTHORIZED);
        }

        Long userId = jwtUtil.parseUserId(token);
        request.setAttribute("userId", userId);
        return true;
    }
}
