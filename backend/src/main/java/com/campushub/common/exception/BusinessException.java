package com.campushub.common.exception;

public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(ApiCode apiCode) {
        super(apiCode.getMessage());
        this.code = apiCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
