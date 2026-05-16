package com.campushub.common.exception;

public enum ApiCode {
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "参数校验失败"),
    UNAUTHORIZED(401, "未认证或凭证失效"),
    FORBIDDEN(403, "无权限操作"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务端异常"),
    ORDER_TAKEN(4001, "手慢了，该需求已被他人接取"),
    SELF_ORDER(4002, "发布者不可接取自身发布的需求");

    private final Integer code;
    private final String message;

    ApiCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
