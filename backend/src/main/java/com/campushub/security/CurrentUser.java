package com.campushub.security;

import com.campushub.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    private final String userHeader;

    public CurrentUser(@Value("${campushub.auth.user-header:X-User-Id}") String userHeader) {
        this.userHeader = userHeader;
    }

    public Long requireUserId(HttpServletRequest request) {
        String raw = request.getHeader(userHeader);
        if (raw == null || raw.isBlank()) {
            raw = parseNumericBearer(request.getHeader("Authorization"));
        }
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(401, "未认证或凭证失效");
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException(401, "未认证或凭证失效");
        }
    }

    private String parseNumericBearer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring("Bearer ".length()).trim();
        // 当前模块先兼容 Bearer 10001 这种联调写法，等登录模块完成后替换为 JWT 解码。
        return token.matches("\\d+") ? token : null;
    }
}
