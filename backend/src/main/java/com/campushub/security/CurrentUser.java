package com.campushub.security;

import com.campushub.common.exception.ApiCode;
import com.campushub.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public Long requireUserId(HttpServletRequest request) {
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr == null) {
            throw new BusinessException(ApiCode.UNAUTHORIZED);
        }
        return (Long) userIdAttr;
    }
}
